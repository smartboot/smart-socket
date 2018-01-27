/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSession.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.StateMachineEnum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * AIO传输层会话
 * Created by 三刀 on 2017/6/29.
 */
public class AioSession<T> {
    /* Session状态:已关闭 */
    protected static final byte SESSION_STATUS_CLOSED = 1,
    /*Session状态:关闭中*/
    SESSION_STATUS_CLOSING = 2,
    /* Session状态:正常 */
    SESSION_STATUS_ENABLED = 3;
    private static final Logger logger = LogManager.getLogger(AioSession.class);
    private static final int MAX_WRITE_SIZE = 256 * 1024;
    /* Session ID生成器 */
    private static int NEXT_ID = 0;
    /**
     * 唯一标识
     */
    private final int sessionId = ++NEXT_ID;
    /**
     * 数据read限流标志,仅服务端需要进行限流
     */
    protected Boolean serverFlowLimit;
    /**
     * 底层通信channel对象
     */
    protected AsynchronousSocketChannel channel;
    protected ByteBuffer readBuffer, writeBuffer;
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
    private ReadCompletionHandler aioReadCompletionHandler;
    private WriteCompletionHandler aioWriteCompletionHandler;
    /**
     * 输出信号量
     */
    private Semaphore semaphore = new Semaphore(1);
    private IoServerConfig<T> ioServerConfig;

    /**
     * @param channel
     * @param config
     * @param aioReadCompletionHandler
     * @param aioWriteCompletionHandler
     * @param serverSession             是否服务端Session
     */
    AioSession(AsynchronousSocketChannel channel, IoServerConfig<T> config, ReadCompletionHandler aioReadCompletionHandler, WriteCompletionHandler aioWriteCompletionHandler, boolean serverSession) {
        this.channel = channel;
        this.aioReadCompletionHandler = aioReadCompletionHandler;
        this.aioWriteCompletionHandler = aioWriteCompletionHandler;
        this.writeCacheQueue = new ArrayBlockingQueue<ByteBuffer>(config.getWriteQueueSize());
        this.ioServerConfig = config;
        this.serverFlowLimit = serverSession ? false : null;
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);//触发状态机
        this.readBuffer = ByteBuffer.allocate(config.getReadBufferSize());
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
        writeBuffer = null;
        if (writeCacheQueue.isEmpty()) {
            semaphore.release();
            //此时可能是Closing或Closed状态
            if (isInvalid()) {
                close();
            }
            //也许此时有新的消息通过write方法添加到writeCacheQueue中
            else if (writeCacheQueue.size() > 0 && semaphore.tryAcquire()) {
                writeToChannel();
            }
            return;
        }
        Iterator<ByteBuffer> iterable = writeCacheQueue.iterator();
        int totalSize = 0;
        while (iterable.hasNext() && totalSize <= MAX_WRITE_SIZE) {
            totalSize += iterable.next().remaining();
        }
        byte[] data = new byte[totalSize];
        int index = 0;
        while (index < data.length) {
            ByteBuffer srcBuffer = writeCacheQueue.poll();
            System.arraycopy(srcBuffer.array(), srcBuffer.position(), data, index, srcBuffer.remaining());
            index += srcBuffer.remaining();
        }
        writeBuffer = ByteBuffer.wrap(data);
        continueWrite();

        //如果存在流控并符合释放条件，则触发读操作
        if (serverFlowLimit != null && serverFlowLimit && writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            serverFlowLimit = false;
            continueRead();
        }
    }

    /**
     * 触发通道的读操作
     *
     * @param buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, aioReadCompletionHandler);
    }

    /**
     * 触发通道的写操作
     *
     * @param buffer
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        channel.write(buffer, this, aioWriteCompletionHandler);
    }

    public void write(final ByteBuffer buffer) throws IOException {
        if (isInvalid()) {
            throw new IOException("session is " + status);
        }
        try {
            //正常读取
            writeCacheQueue.put(buffer);
        } catch (InterruptedException e) {
            logger.error(e);
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
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }
        status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
        if (immediate) {
            try {
                channel.close();
                if (logger.isDebugEnabled()) {
                    logger.debug("session is closed:" + this);
                }
            } catch (IOException e) {
                logger.catching(e);
            }
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
        } else if ((writeBuffer == null || !writeBuffer.hasRemaining()) && writeCacheQueue.isEmpty() && semaphore.tryAcquire()) {
            close(true);
            semaphore.release();
        } else {
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
        }
    }


    /**
     * 获取当前Session的唯一标识
     *
     * @return
     */
    public final int getSessionID() {
        return sessionId;
    }

    /**
     * 当前会话是否已失效
     */
    public boolean isInvalid() {
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
                logger.catching(e);
                for (Filter<T> h : ioServerConfig.getFilters()) {
                    h.processFailHandler(this, dataEntry, e);
                }
            }

        }

        if (eof || status == SESSION_STATUS_CLOSING) {
            if (readBuffer.hasRemaining()) {
                logger.error("{} bytes has not decode when EOF", readBuffer.remaining());
            }
            close(false);
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.INPUT_SHUTDOWN, null);
            return;
        }

        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {// 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }

        if (serverFlowLimit != null && serverFlowLimit) {
            throw new RuntimeException("不该出现的情况");
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

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public final void write(T t) throws IOException {
        write(ioServerConfig.getProtocol().encode(t, this));
    }

    public InetSocketAddress getLocalAddress() {
        try {
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InetSocketAddress getRemoteAddress() {
        try {
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    IoServerConfig getServerConfig() {
        return this.ioServerConfig;
    }

}
