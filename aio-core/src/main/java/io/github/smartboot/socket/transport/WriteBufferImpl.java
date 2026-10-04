/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteBufferImpl.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.transport;

import io.github.smartboot.socket.buffer.BufferPage;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/11/8
 */

final class WriteBufferImpl implements WriteBuffer {
    /**
     * 存储已就绪待输出的数据
     */
    private final VirtualBuffer[] items;

    /**
     * 为当前 WriteBuffer 提供数据存放功能的缓存页
     */
    private final BufferPage bufferPage;
    /**
     * 缓冲区数据刷新Function
     */
    private final Consumer<VirtualBuffer> writeConsumer;
    /**
     * 默认内存块大小
     */
    private final int chunkSize;
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
     * 当前WriteBuffer是否已关闭
     */
    private boolean closed = false;
    /**
     * 辅助8字节以内输出的缓存组数
     */
    private byte[] cacheByte;

    /**
     * 输出信号量,防止并发write导致异常
     */
    private final Semaphore semaphore = new Semaphore(1);

    WriteBufferImpl(BufferPagePool pool, Consumer<VirtualBuffer> writeConsumer, int chunkSize, int capacity) {
        this.bufferPage = pool.allocatePage();
        this.writeConsumer = writeConsumer;
        this.items = new VirtualBuffer[capacity];
        this.chunkSize = chunkSize;
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
        cacheByte[1] = (byte) (v & 0xFF);
        write(cacheByte, 0, 2);
    }

    /**
     * @param b 待输出数值
     */
    public synchronized void writeByte(byte b) {
        if (writeInBuf == null) {
            writeInBuf = bufferPage.allocate(chunkSize);
        }
        writeInBuf.buffer().put(b);
        flushWriteBuffer(false);
    }

    private void flushWriteBuffer(boolean forceFlush) {
        if (!forceFlush && writeInBuf.buffer().hasRemaining()) {
            return;
        }
        writeInBuf.buffer().flip();
        VirtualBuffer virtualBuffer = writeInBuf;
        writeInBuf = null;
        offerWriteBuffer(virtualBuffer);
    }

    /**
     * 将待输出数据投入输出队列：队列空闲则直接输出，否则缓存至队尾;
     * 队列已满时阻塞等待，直至出现空位或 WriteBuffer 被关闭。
     *
     * @param virtualBuffer 待输出的数据
     */
    private void offerWriteBuffer(VirtualBuffer virtualBuffer) {
        if (count == 0 && semaphore.tryAcquire()) {
            writeConsumer.accept(virtualBuffer);
            return;
        }

        try {
            while (count == items.length) {
                this.wait();
                //防止因close诱发内存泄露
                if (closed) {
                    virtualBuffer.clean();
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
        } finally {
            flush();
        }
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
        cacheByte[3] = (byte) (v & 0xFF);
        write(cacheByte, 0, 4);
    }

    /**
     * 输出long数值,占用8个字节
     *
     * @param v long数值
     * @throws IOException IO异常
     */
    public void writeLong(long v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 56) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 48) & 0xFF);
        cacheByte[2] = (byte) ((v >>> 40) & 0xFF);
        cacheByte[3] = (byte) ((v >>> 32) & 0xFF);
        cacheByte[4] = (byte) ((v >>> 24) & 0xFF);
        cacheByte[5] = (byte) ((v >>> 16) & 0xFF);
        cacheByte[6] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[7] = (byte) (v & 0xFF);
        write(cacheByte, 0, 8);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            if (closed) {
                throw new IOException("writeBuffer has closed");
            }
            if (writeInBuf == null) {
                writeInBuf = bufferPage.allocate(chunkSize);
            }
            ByteBuffer writeBuffer = writeInBuf.buffer();
            int remaining = writeBuffer.remaining();
            if (remaining > len) {
                writeBuffer.put(b, off, len);
                break;
            }
            writeBuffer.put(b, off, remaining);
            flushWriteBuffer(true);
            off += remaining;
            len -= remaining;
        }
    }

    /**
     * 输出指定的ByteBuffer,数据由框架接管直至写出或因会话关闭被丢弃,
     * 之后触发releaseCallback,调用方可安全复用该buffer。
     * <p>注意:回调触发不代表数据已成功送达对端。</p>
     *
     * @param byteBuffer     待输出的数据
     * @param releaseCallback buffer释放回调
     */
    public synchronized void write(ByteBuffer byteBuffer, Runnable releaseCallback) {
        if (!byteBuffer.hasRemaining()) {
            throw new IllegalStateException("none remaining byteBuffer");
        }
        if (writeInBuf != null && writeInBuf.buffer().position() > 0) {
            flushWriteBuffer(true);
        }
        offerWriteBuffer(VirtualBuffer.wrap(byteBuffer, releaseCallback));
    }

    /**
     * 初始化8字节的缓存数值
     */
    private void initCacheBytes() {
        if (cacheByte == null) {
            cacheByte = new byte[8];
        }
    }

    @Override
    public void flush() {
        if (closed) {
            throw new RuntimeException("OutputStream has closed");
        }
        //快速检测，无数据时直接返回
        if (count == 0 && writeInBuf == null) {
            return;
        }
        if (semaphore.tryAcquire()) {
            VirtualBuffer virtualBuffer = poll();
            if (virtualBuffer == null) {
                semaphore.release();
            } else {
                writeConsumer.accept(virtualBuffer);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        flush();
        closed = true;
        if (writeInBuf != null) {
            writeInBuf.clean();
            writeInBuf = null;
        }
        VirtualBuffer byteBuf;
        while ((byteBuf = poll()) != null) {
            byteBuf.clean();
        }
    }


    /**
     * 是否存在待输出的数据
     *
     * @return true:有,false:无
     */
    synchronized boolean isEmpty() {
        return count == 0 && (writeInBuf == null || writeInBuf.buffer().position() == 0);
    }

    void finishWrite() {
        semaphore.release();
    }

    private VirtualBuffer pollItem() {
        if (count == 0) {
            return null;
        }
        VirtualBuffer x = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        if (count-- == items.length) {
            this.notifyAll();
        }
        return x;
    }

    /**
     * 获取并移除当前缓冲队列中头部的VirtualBuffer
     *
     * @return 待输出的VirtualBuffer
     */
    synchronized VirtualBuffer poll() {
        VirtualBuffer item = pollItem();
        if (item != null) {
            return item;
        }
        item = writeInBuf;
        if (writeInBuf != null) {
            writeInBuf.buffer().flip();
            writeInBuf = null;
        }
        return item;
    }

}