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
import org.smartboot.socket.Filter;
import org.smartboot.socket.StateMachineEnum;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * AIO传输层会话
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
     * 数据read限流标志,仅服务端需要进行限流
     */
    protected Boolean serverFlowLimit;
    /**
     * 底层通信channel对象
     */
    protected AsynchronousSocketChannel channel;
    protected ByteBuffer readBuffer;
    protected ByteBuffer writeBuffer;
    /**
     * 会话当前状态
     */
    protected byte status = SESSION_STATUS_ENABLED;
    /**
     * 附件对象
     */
    private Object attachment;

    /**
     * 响应消息缓存队列
     */
    private ArrayBlockingQueue<ByteBuffer> writeCacheQueue;
    private ReadCompletionHandler readCompletionHandler;
    private WriteCompletionHandler writeCompletionHandler;
    /**
     * 输出信号量
     */
    private Semaphore semaphore = new Semaphore(1);
    private IoServerConfig<T> ioServerConfig;
    private InputStream inputStream;

    /**
     * @param channel
     * @param config
     * @param readCompletionHandler
     * @param writeCompletionHandler
     * @param serverSession          是否服务端Session
     */
    AioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler readCompletionHandler, WriteCompletionHandler writeCompletionHandler, boolean serverSession) {
        this.channel = channel;
        this.readCompletionHandler = readCompletionHandler;
        this.writeCompletionHandler = writeCompletionHandler;
        if (config.getWriteQueueSize() > 0) {
            this.writeCacheQueue = new ArrayBlockingQueue<>(config.getWriteQueueSize());
        }
        this.ioServerConfig = config;
        this.serverFlowLimit = serverSession && config.getWriteQueueSize() > 0 ? false : null;
        //触发状态机
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
        this.readBuffer = newByteBuffer0(config.getReadBufferSize());
        for (Filter<T> filter : config.getFilters()) {
            filter.connected(this);
        }
    }

    /**
     * 初始化AioSession
     */
    public void initSession() {
        continueRead();
    }

    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeToChannel() {
        if (writeBuffer != null && writeBuffer.hasRemaining()) {
            continueWrite();
            return;
        }

        if (writeCacheQueue == null || writeCacheQueue.isEmpty()) {
            writeBuffer = null;
            semaphore.release();
            //此时可能是Closing或Closed状态
            if (isInvalid()) {
                close();
            }
            //也许此时有新的消息通过write方法添加到writeCacheQueue中
            else if (writeCacheQueue != null && writeCacheQueue.size() > 0 && semaphore.tryAcquire()) {
                writeToChannel();
            }
            return;
        }
        Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
        int totalSize = 0;
        while (iterable.hasNext() && totalSize <= MAX_WRITE_SIZE) {
            totalSize += iterable.next().remaining();
        }
        ByteBuffer headBuffer = writeCacheQueue.poll();
        if (headBuffer.remaining() == totalSize) {
            writeBuffer = headBuffer;
        } else {
            if (writeBuffer == null || totalSize << 1 <= writeBuffer.capacity() || totalSize > writeBuffer.capacity()) {
                writeBuffer = newByteBuffer0(totalSize);
            } else {
                writeBuffer.clear().limit(totalSize);
            }
            writeBuffer.put(headBuffer);
            while (writeBuffer.hasRemaining()) {
                writeBuffer.put(writeCacheQueue.poll());
            }
            writeBuffer.flip();
        }

        //如果存在流控并符合释放条件，则触发读操作
        //一定要放在continueWrite之前
        if (serverFlowLimit != null && serverFlowLimit && writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            serverFlowLimit = false;
            continueRead();
        }
        continueWrite();

    }

    /**
     * 触发通道的读操作
     *
     * @param buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, readCompletionHandler);
    }

    /**
     * 触发通道的写操作
     *
     * @param buffer
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        channel.write(buffer, this, writeCompletionHandler);
    }

    public final void write(final ByteBuffer buffer) throws IOException {
        if (isInvalid()) {
            throw new IOException("session is " + (status == SESSION_STATUS_CLOSED ? "closed" : "invalid"));
        }
        if (!buffer.hasRemaining()) {
            throw new InvalidObjectException("buffer has no remaining");
        }
        if (ioServerConfig.getWriteQueueSize() <= 0) {
            try {
                semaphore.acquire();
                writeBuffer = buffer;
                continueWrite();
            } catch (InterruptedException e) {
                logger.error("acquire fail", e);
                Thread.currentThread().interrupt();
                throw new IOException(e.getMessage());
            }
            return;
        } else if ((semaphore.tryAcquire())) {
            writeBuffer = buffer;
            continueWrite();
            return;
        }
        try {
            //正常读取
            writeCacheQueue.put(buffer);
        } catch (InterruptedException e) {
            logger.error("put buffer into cache fail", e);
            Thread.currentThread().interrupt();
        }
        if (semaphore.tryAcquire()) {
            writeToChannel();
        }
    }

    public final void close() {
        close(true);
    }

    /**
     * * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public void close(boolean immediate) {
        //status == SESSION_STATUS_CLOSED说明close方法被重复调用
        if (status == SESSION_STATUS_CLOSED) {
            logger.warn("ignore, session:{} is closed:", getSessionID());
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
                if (logger.isDebugEnabled()) {
                    logger.debug("session:{} is closed:", getSessionID());
                }
                channel = null;
            } catch (IOException e) {
                logger.debug("close session exception", e);
            }
            for (Filter<T> filter : ioServerConfig.getFilters()) {
                filter.closed(this);
            }
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
        } else if ((writeBuffer == null || !writeBuffer.hasRemaining()) && (writeCacheQueue == null || writeCacheQueue.isEmpty()) && semaphore.tryAcquire()) {
            close(true);
            semaphore.release();
        } else {
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
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
        readBuffer.flip();

        T dataEntry;
        while ((dataEntry = ioServerConfig.getProtocol().decode(readBuffer, this, eof)) != null) {
            //处理消息
            try {
                for (Filter<T> h : ioServerConfig.getFilters()) {
                    h.processFilter(this, dataEntry);
                }
                ioServerConfig.getProcessor().process(this, dataEntry);
            } catch (Exception e) {
                ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.PROCESS_EXCEPTION, e);
                for (Filter<T> h : ioServerConfig.getFilters()) {
                    h.processFail(this, dataEntry, e);
                }
            }

        }

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

        //触发流控
        if (serverFlowLimit != null && writeCacheQueue.size() > ioServerConfig.getFlowLimitLine()) {
            serverFlowLimit = true;
        } else {
            continueRead();
        }
    }

    protected void continueRead() {
        readFromChannel0(readBuffer);
    }

    protected void continueWrite() {
        writeToChannel0(writeBuffer);
    }

    public final Object getAttachment() {
        return attachment;
    }

    public final void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public final void write(T t) throws IOException {
        write(ioServerConfig.getProtocol().encode(t, this));
    }

    public final InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) channel.getLocalAddress();
    }

    public final InetSocketAddress getRemoteAddress() throws IOException {
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    IoServerConfig getServerConfig() {
        return this.ioServerConfig;
    }

    private ByteBuffer newByteBuffer0(int size) {
        return ioServerConfig.isDirectBuffer() ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    /**
     * 获得数据输入流对象
     *
     * @return
     */
    public InputStream getInputStream() throws IOException {
        return inputStream == null ? getInputStream(-1) : inputStream;
    }

    /**
     * 获取已知长度的InputStream
     *
     * @param length
     * @return
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
            return remainLength == 0 ? 0 : readBuffer.remaining();
        }

        @Override
        public void close() throws IOException {
            if (AioSession.this.inputStream == InnerInputStream.this) {
                AioSession.this.inputStream = null;
            }
        }
    }
}
