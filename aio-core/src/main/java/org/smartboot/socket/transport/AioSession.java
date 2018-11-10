/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSession.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * AIO传输层会话。
 *
 * <p>
 * AioSession为smart-socket最核心的类，封装{@link AsynchronousSocketChannel} API接口，简化IO操作。
 * </p>
 * <p>
 * 其中开放给用户使用的接口为：
 * <ol>
 * <li>{@link AioSession#close()}</li>
 * <li>{@link AioSession#close(boolean)}</li>
 * <li>{@link AioSession#getAttachment()} </li>
 * <li>{@link AioSession#getInputStream()} </li>
 * <li>{@link AioSession#getInputStream(int)} </li>
 * <li>{@link AioSession#getLocalAddress()} </li>
 * <li>{@link AioSession#getRemoteAddress()} </li>
 * <li>{@link AioSession#getSessionID()} </li>
 * <li>{@link AioSession#isInvalid()} </li>
 * <li>{@link AioSession#setAttachment(Object)}  </li>
 * <li>{@link AioSession#write(ByteBuf)} </li>
 * <li>{@link AioSession#write(Object)}   </li>
 * </ol>
 *
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioSession<T> {
    /**
     * Session状态:已关闭
     */
    protected static final byte SESSION_STATUS_CLOSED = 1;
    /**
     * Session状态:关闭中
     */
    protected static final byte SESSION_STATUS_CLOSING = 2;
    /**
     * Session状态:正常
     */
    protected static final byte SESSION_STATUS_ENABLED = 3;
    private static final Logger logger = LoggerFactory.getLogger(AioSession.class);
    private static final int MAX_WRITE_SIZE = 256 * 1024;

    /**
     * 底层通信channel对象
     */
    protected AsynchronousSocketChannel channel;
    /**
     * 读缓冲。
     * <p>大小取决于AioQuickClient/AioQuickServer设置的setReadBufferSize</p>
     */
    protected ByteBuf readBuffer;
    /**
     * 写缓冲
     */
    protected ByteBuf writeBuffer;
    /**
     * 会话当前状态
     *
     * @see AioSession#SESSION_STATUS_CLOSED
     * @see AioSession#SESSION_STATUS_CLOSING
     * @see AioSession#SESSION_STATUS_ENABLED
     */
    protected byte status = SESSION_STATUS_ENABLED;
    Semaphore readSemaphore = new Semaphore(1);
    private BufferPage bufferPage;
    /**
     * 附件对象
     */
    private Object attachment;
    /**
     * 是否流控,客户端写流控，服务端读流控
     */
    private boolean flowControl;
    /**
     * 响应消息缓存队列。
     * <p>长度取决于AioQuickClient/AioQuickServer设置的setWriteQueueSize</p>
     */
    private ReadCompletionHandler<T> readCompletionHandler;
    private WriteCompletionHandler<T> writeCompletionHandler;
    /**
     * 输出信号量
     */
    private Semaphore semaphore = new Semaphore(1);
    private IoServerConfig<T> ioServerConfig;
    private InputStream inputStream;
    private BufferOutputStream outputStream;

    /**
     * @param channel
     * @param config
     * @param readCompletionHandler
     * @param writeCompletionHandler
     * @param bufferPage             是否服务端Session
     */
    AioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler<T> readCompletionHandler, WriteCompletionHandler<T> writeCompletionHandler, BufferPage bufferPage) {
        this.channel = channel;
        this.readCompletionHandler = readCompletionHandler;
        this.writeCompletionHandler = writeCompletionHandler;
        this.ioServerConfig = config;
        this.bufferPage = bufferPage;
        //触发状态机
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
        this.readBuffer = bufferPage.allocate(config.getReadBufferSize());
        outputStream = new BufferOutputStream();
    }

    /**
     * 初始化AioSession
     */
    void initSession() {
        readSemaphore.tryAcquire();
        continueRead();
    }

    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeToChannel() {
        if (writeBuffer != null && writeBuffer.buffer().hasRemaining()) {
            continueWrite();
            return;
        }
        if (writeBuffer != null) {
            writeBuffer.release();
        }
        writeBuffer = null;
        if (outputStream.bufList.isEmpty()) {
            semaphore.release();
            //此时可能是Closing或Closed状态
            if (isInvalid()) {
                close();
            }
            //也许此时有新的消息通过write方法添加到writeCacheQueue中
            else if (!outputStream.bufList.isEmpty()) {
                outputStream.flush();
            }
            return;
        }

        //如果存在流控并符合释放条件，则触发读操作
        //一定要放在continueWrite之前

        if (flowControl && outputStream.bufList.size() < ioServerConfig.getReleaseLine()) {
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.RELEASE_FLOW_LIMIT, null);
            flowControl = false;
            readFromChannel(false);
        }


        AioSession.this.writeBuffer = outputStream.bufList.poll();
        continueWrite();


    }

    /**
     * 内部方法：触发通道的读操作
     *
     * @param buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, readCompletionHandler);
    }

    /**
     * 内部方法：触发通道的写操作
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        channel.write(buffer, this, writeCompletionHandler);
    }

    public final OutputStream getOutputStream() {
        return outputStream;
    }

    ;

    /**
     * 将数据buffer输出至网络对端。
     * <p>
     * 若当前无待输出的数据，则立即输出buffer.
     * </p>
     * <p>
     * 若当前存在待数据数据，且无可用缓冲队列(writeCacheQueue)，则阻塞。
     * </p>
     * <p>
     * 若当前存在待输出数据，且缓冲队列存在可用空间，则将buffer存入writeCacheQueue。
     * </p>
     *
     * @param buffer
     * @throws IOException
     */
//    public final void write(final ByteBuf buffer) throws IOException {
//        if (isInvalid()) {
//            throw new IOException("session is " + (status == SESSION_STATUS_CLOSED ? "closed" : "invalid"));
//        }
//        if (!buffer.buffer().hasRemaining()) {
//            throw new InvalidObjectException("buffer has no remaining");
//        }
//        if (ioServerConfig.getWriteQueueSize() <= 0) {
//            try {
//                semaphore.acquire();
//                writeBuffer = buffer;
//                continueWrite();
//            } catch (InterruptedException e) {
//                logger.error("acquire fail", e);
//                Thread.currentThread().interrupt();
//                throw new IOException(e.getMessage());
//            }
//            return;
//        } else if ((semaphore.tryAcquire())) {
//            writeBuffer = buffer;
//            continueWrite();
//            return;
//        }
//        try {
//            //正常读取
//            writeCacheQueue.put(buffer);
//            if (writeCacheQueue.size() >= ioServerConfig.getFlowLimitLine() && ioServerConfig.isServer()) {
//                flowControl = true;
//                ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.FLOW_LIMIT, null);
//            }
//        } catch (InterruptedException e) {
//            logger.error("put buffer into cache fail", e);
//            Thread.currentThread().interrupt();
//        }
//        if (semaphore.tryAcquire()) {
//            writeToChannel();
//        }
//    }

    /**
     * 强制关闭当前AIOSession。
     * <p>若此时还存留待输出的数据，则会导致该部分数据丢失</p>
     */
    public final void close() {
        close(true);
    }

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public synchronized void close(boolean immediate) {
        //status == SESSION_STATUS_CLOSED说明close方法被重复调用
        if (status == SESSION_STATUS_CLOSED) {
            logger.warn("ignore, session:{} is closed:", getSessionID());
            semaphore.release();
            return;
        }
        status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
        if (immediate) {
            try {
                channel.shutdownInput();
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            }
            try {
                channel.shutdownOutput();
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            }
            try {
                channel.close();
            } catch (IOException e) {
                logger.debug("close session exception", e);
            }
            try {
                ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
            } finally {
                semaphore.release();
            }
            readBuffer.release();
            readBuffer = null;
//            if (writeBuffer != null) {
//                writeBuffer.release();
//                writeBuffer = null;
//            }
//            outputStream.close();

        } else if ((writeBuffer == null || !writeBuffer.buffer().hasRemaining()) && outputStream.bufList.isEmpty() && outputStream.writeBuf == null && semaphore.tryAcquire()) {
            close(true);
        } else {
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
            if (outputStream.writeBuf != null) {
                outputStream.flush();
            }
        }
    }

    /**
     * 获取当前Session的唯一标识
     */
    public final String getSessionID() {
        return "aioSession-" + hashCode();
    }

    /**
     * 当前会话是否已失效
     */
    public final boolean isInvalid() {
        return status != SESSION_STATUS_ENABLED;
    }

    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    void readFromChannel(boolean eof) {
        //处于流控状态
        if (flowControl || !readSemaphore.tryAcquire()) {
            return;
        }
        ByteBuffer readBuffer = this.readBuffer.buffer();
        readBuffer.flip();


        while (readBuffer.hasRemaining() && !isInvalid()) {
            T dataEntry = null;
            try {
                dataEntry = ioServerConfig.getProtocol().decode(readBuffer, this);
            } catch (Exception e) {
                ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, e);
                throw e;
            }
            if (dataEntry == null) {
                break;
            }

            //处理消息
            try {
                ioServerConfig.getProcessor().process(this, dataEntry);
            } catch (Exception e) {
                ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.PROCESS_EXCEPTION, e);
            }
        }
        outputStream.flush();

        if (eof || status == SESSION_STATUS_CLOSING) {
            close(false);
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.INPUT_SHUTDOWN, null);
            return;
        }
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }

        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {
            // 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }
        continueRead();
    }


    protected void continueRead() {
        readFromChannel0(readBuffer.buffer());
    }

    protected void continueWrite() {
        writeToChannel0(writeBuffer.buffer());
    }

    /**
     * 获取附件对象
     *
     * @return
     */
    public final <T> T getAttachment() {
        return (T) attachment;
    }

    /**
     * 存放附件，支持任意类型
     */
    public final <T> void setAttachment(T attachment) {
        this.attachment = attachment;
    }

//    /**
//     * 输出消息。
//     * <p>必须实现{@link org.smartboot.socket.Protocol#encode(Object, AioSession)}</p>方法
//     *
//     * @param t 待输出消息必须为当前服务指定的泛型
//     * @throws IOException
//     */
//    public final void write(T t) throws IOException {
//        getOutputStream().write(ioServerConfig.getProtocol().encode(t, this));
//    }

    /**
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public final InetSocketAddress getLocalAddress() throws IOException {
        assertChannel();
        return (InetSocketAddress) channel.getLocalAddress();
    }

    /**
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public final InetSocketAddress getRemoteAddress() throws IOException {
        assertChannel();
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    private void assertChannel() throws IOException {
        if (status == SESSION_STATUS_CLOSED || channel == null) {
            throw new IOException("session is closed");
        }
    }

    public ByteBuf allocateBuf(int size) {
        return bufferPage.allocate(size);
    }

    IoServerConfig<T> getServerConfig() {
        return this.ioServerConfig;
    }

    /**
     * 获得数据输入流对象。
     * <p>
     * faster模式下调用该方法会触发UnsupportedOperationException异常。
     * </p>
     * <p>
     * MessageProcessor采用异步处理消息的方式时，调用该方法可能会出现异常。
     * </p>
     */
    public InputStream getInputStream() throws IOException {
        return inputStream == null ? getInputStream(-1) : inputStream;
    }

    /**
     * 获取已知长度的InputStream
     *
     * @param length InputStream长度
     */
    public InputStream getInputStream(int length) throws IOException {
        if (inputStream != null) {
            throw new IOException("pre inputStream has not closed");
        }
        if (inputStream != null) {
            return inputStream;
        }
        synchronized (this) {
            if (inputStream == null) {
                inputStream = new InnerInputStream(length);
            }
        }
        return inputStream;
    }

    /**
     * @author 三刀
     * @version V1.0 , 2018/11/8
     */
    private class BufferOutputStream extends OutputStream {
        private LinkedBlockingQueue<ByteBuf> bufList = new LinkedBlockingQueue<>();
        private ByteBuf writeBuf;
        private BufferPage bufferPage = AioSession.this.bufferPage;

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
//            System.out.println("1");
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            if (writeBuf == null) {
                writeBuf = bufferPage.allocate(1024);
            }
            ByteBuffer writeBuffer = writeBuf.buffer();
            do {
                int minSize = Math.min(writeBuffer.remaining(), len - off);
                writeBuffer.put(b, off, minSize);
                off += minSize;
                if (!writeBuffer.hasRemaining()) {
                    writeBuffer.flip();
                    flush();
                    if (off < len) {
                        writeBuf = bufferPage.allocate(1024);
                    } else {
                        writeBuf = null;
                    }
                }
            } while (off < len);
        }


        @Override
        public void flush() {
            if (writeBuf != null && writeBuf.buffer().position() > 0) {
                writeBuf.buffer().flip();
                bufList.add(writeBuf);
                writeBuf = null;
            }

            if (!AioSession.this.semaphore.tryAcquire()) {
                return;
            }
            ByteBuf byteBuf = bufList.poll();
            if (byteBuf == null) {
                AioSession.this.semaphore.release();
            } else {
                AioSession.this.writeBuffer = byteBuf;
                continueWrite();
            }
        }

        @Override
        public void close() {
            ByteBuf byteBuf = null;
            while ((byteBuf = bufList.poll()) != null) {
                byteBuf.release();
            }
            bufList = null;
            if (writeBuf != null) {
                writeBuf.release();
                writeBuf = null;
            }
        }
    }

    private class InnerInputStream extends InputStream {
        private int remainLength;

        public InnerInputStream(int length) {
            this.remainLength = length >= 0 ? length : -1;
        }

        @Override
        public int read() throws IOException {
            if (remainLength == 0) {
                return -1;
            }
            ByteBuffer readBuffer = AioSession.this.readBuffer.buffer();
            if (readBuffer.hasRemaining()) {
                remainLength--;
                return readBuffer.get();
            }
            readBuffer.clear();

            try {
                int readSize = channel.read(readBuffer).get();
                readBuffer.flip();
                if (readSize == -1) {
                    remainLength = 0;
                    return -1;
                } else {
                    return read();
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public int available() throws IOException {
            return remainLength == 0 ? 0 : readBuffer.buffer().remaining();
        }

        @Override
        public void close() throws IOException {
            if (AioSession.this.inputStream == InnerInputStream.this) {
                AioSession.this.inputStream = null;
            }
        }
    }
}
