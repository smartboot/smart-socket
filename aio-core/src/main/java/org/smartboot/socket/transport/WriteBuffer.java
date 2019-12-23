package org.smartboot.socket.transport;

import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/11/8
 */

public class WriteBuffer extends OutputStream {
    /**
     * 存储已就绪待输出的数据
     */
    private final VirtualBuffer[] items;
    /**
     * 同步锁
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * Condition for waiting puts
     */
    private final Condition notFull = lock.newCondition();
    /**
     * 当缓冲队列已满时，触发线程阻塞条件
     */
    private final Condition waiting = lock.newCondition();
    /**
     * 为当前 WriteBuffer 提供数据存放功能的缓存页
     */
    private final BufferPage bufferPage;
    /**
     * 缓冲区数据刷新Function
     */
    private final Function<WriteBuffer, Void> function;
    /**
     * 当时是否符合wait条件
     */
    private volatile boolean isWaiting = false;
    /**
     * items 读索引位
     */
    private int takeIndex;
    /**
     * items 写索引位
     */
    private int putIndex;
    /**
     * items 中存放的缓冲数据数量
     */
    private int count;
    /**
     * 暂存当前业务正在输出的数据,输出完毕后会存放到items中
     */
    private VirtualBuffer writeInBuf;

    /**
     * 暂存当前待输出的数据
     */
    private volatile VirtualBuffer writeOutBuf;

    /**
     * 当前WriteBuffer是否已关闭
     */
    private boolean closed = false;
    /**
     * 辅助8字节以内输出的缓存组数
     */
    private byte[] cacheByte;
    /**
     * 数据快速输出
     */
    private FasterWrite fasterWrite;

    /**
     * 默认内存块大小
     */
    private int chunkSize;


    protected WriteBuffer(BufferPage bufferPage, Function<WriteBuffer, Void> flushFunction, IoServerConfig config, FasterWrite fasterWrite) {
        this.bufferPage = bufferPage;
        this.function = flushFunction;
        this.items = new VirtualBuffer[config.getWriteQueueCapacity()];
        this.fasterWrite = fasterWrite == null ? new FasterWrite() : fasterWrite;
        this.chunkSize = config.getBufferPoolChunkSize();
    }

    /**
     * 按照{@link OutputStream#write(int)}规范：要写入的字节是参数 b 的八个低位。 b 的 24 个高位将被忽略。
     * <br/>
     * 而使用该接口时容易传入非byte范围内的数据，接口定义与实际使用出现歧义的可能性较大，故建议废弃该方法，选用{@link WriteBuffer#writeByte(byte)}。
     *
     * @param b
     * @throws IOException 如果发生 I/O 错误
     * @deprecated
     */
    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }


    /**
     * 输出一个short类型的数据
     *
     * @param v short数值
     * @throws IOException IO异常
     */
    public void writeShort(short v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 0) & 0xFF);
        write(cacheByte, 0, 2);
    }

    /**
     * @param b 待输出数值
     * @see #write(int)
     */
    public void writeByte(byte b) {
        if (writeInBuf == null) {
            writeInBuf = bufferPage.allocate(chunkSize);
        }
        writeInBuf.buffer().put(b);
        if (writeInBuf.buffer().hasRemaining()) {
            return;
        }
        writeInBuf.buffer().flip();
        lock.lock();
        try {
            this.put(writeInBuf);
        } finally {
            lock.unlock();
        }
        writeInBuf = null;
        function.apply(this);
    }

    /**
     * 输出int数值,占用4个字节
     *
     * @param v int数值
     * @throws IOException IO异常
     */
    public void writeInt(int v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 24) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 16) & 0xFF);
        cacheByte[2] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[3] = (byte) ((v >>> 0) & 0xFF);
        write(cacheByte, 0, 4);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("OutputStream has closed");
        }
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        lock.lock();
        try {
            waitPreWriteFinish();
            do {
                if (writeInBuf == null) {
                    writeInBuf = bufferPage.allocate(Math.max(chunkSize, len - off));
                }
                ByteBuffer writeBuffer = writeInBuf.buffer();
                int minSize = Math.min(writeBuffer.remaining(), len - off);
                if (minSize == 0 || closed) {
                    writeInBuf.clean();
                    throw new IOException("writeBuffer.remaining:" + writeBuffer.remaining() + " closed:" + closed);
                }
                writeBuffer.put(b, off, minSize);
                off += minSize;
                if (!writeBuffer.hasRemaining()) {
                    writeBuffer.flip();
                    VirtualBuffer buffer = writeInBuf;
                    writeInBuf = null;
                    this.put(buffer);
                    function.apply(this);
                }
            } while (off < len);
            notifyWaiting();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 唤醒处于waiting状态的线程
     */
    private void notifyWaiting() {
        isWaiting = false;
        waiting.signal();
    }

    /**
     * 初始化8字节的缓存数值
     */
    private void initCacheBytes() {
        if (cacheByte == null) {
            cacheByte = new byte[8];
        }
    }

    /**
     * 确保数据输出有序性
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void waitPreWriteFinish() throws IOException {
        while (isWaiting) {
            try {
                waiting.await();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * 写入内容并刷新缓冲区。在{@link org.smartboot.socket.MessageProcessor#process(AioSession, Object)}执行的write操作可无需调用该方法，业务执行完毕后框架本身会自动触发flush。
     * 调用该方法后数据会及时的输出到对端，如果再循环体中通过该方法往某个通道中写入数据将无法获得最佳性能表现，
     *
     * @param b 待输出数据
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeAndFlush(byte[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        writeAndFlush(b, 0, b.length);
    }

    /**
     * @param b   待输出数据
     * @param off b的起始位点
     * @param len 从b中输出的数据长度
     * @throws IOException 如果发生 I/O 错误
     * @see WriteBuffer#writeAndFlush(byte[])
     */
    public void writeAndFlush(byte[] b, int off, int len) throws IOException {
        write(b, off, len);
        flush();
    }

    @Override
    public void flush() {
        if (closed) {
            throw new RuntimeException("OutputStream has closed");
        }
        int size = this.count;
        if (size > 0 || writeOutBuf != null) {
            function.apply(this);
        } else if (writeInBuf != null && writeInBuf.buffer().position() > 0 && lock.tryLock()) {
            boolean fastWrite = false;
            VirtualBuffer buffer = null;
            try {
                if (writeInBuf != null && writeInBuf.buffer().position() > 0) {
                    buffer = writeInBuf;
                    writeInBuf = null;
                    buffer.buffer().flip();
                    fastWrite = fasterWrite.tryAcquire();
                    if (!fastWrite) {
                        this.put(buffer);
                        size++;
                    }
                }
            } finally {
                lock.unlock();
            }
            if (fastWrite) {
                fasterWrite.write(buffer);
            } else if (size > 0) {
                function.apply(this);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        lock.lock();
        try {
            flush();
            closed = true;
            VirtualBuffer byteBuf;
            while ((byteBuf = poll()) != null) {
                byteBuf.clean();
            }
            if (writeInBuf != null) {
                writeInBuf.clean();
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 是否存在待输出的数据
     *
     * @return true:有,false:无
     */
    boolean hasData() {
        return count > 0 || (writeInBuf != null && writeInBuf.buffer().position() > 0);
    }


    /**
     * 存储缓冲区至队列中以备输出
     *
     * @param virtualBuffer 缓存对象
     */
    private void put(VirtualBuffer virtualBuffer) {
        try {
            while (count == items.length) {
                isWaiting = true;
                notFull.await();
                //防止因close诱发内存泄露
                if (closed) {
                    virtualBuffer.clean();
                    return;
                }
            }
            if (writeOutBuf == null) {
                if (count > 0) {
                    writeOutBuf = items[takeIndex];
                    items[takeIndex] = null;
                    if (++takeIndex == items.length) {
                        takeIndex = 0;
                    }
                    if (count-- == items.length) {
                        notFull.signal();
                    }
                } else {
                    writeOutBuf = virtualBuffer;
                    return;
                }
            }

            items[putIndex] = virtualBuffer;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            count++;
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * 获取并移除当前缓冲队列中头部的VirtualBuffer
     *
     * @return 待输出的VirtualBuffer
     */
    VirtualBuffer poll() {
        if (writeOutBuf != null) {
            VirtualBuffer virtualBuffer = writeOutBuf;
            writeOutBuf = null;
            return virtualBuffer;
        }
        lock.lock();
        try {
            if (writeOutBuf != null) {
                VirtualBuffer virtualBuffer = writeOutBuf;
                writeOutBuf = null;
                return virtualBuffer;
            }
            if (count == 0) {
                return null;
            }
            VirtualBuffer x = items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            if (count-- == items.length) {
                notFull.signal();
            }
            if (count > 0) {
                writeOutBuf = items[takeIndex];
                items[takeIndex] = null;
                if (++takeIndex == items.length) {
                    takeIndex = 0;
                }
                count--;
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

}