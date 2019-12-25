/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSession.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;


import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * AIO传输层会话。
 *
 * <p>
 * AioSession为smart-socket最核心的类，封装{@link AsynchronousSocketChannel} API接口，简化IO操作。
 * </p>
 * <p>
 * 其中开放给用户使用的接口为：
 * <ol>
 * <li>{@link TcpAioSession#close()}</li>
 * <li>{@link TcpAioSession#close(boolean)}</li>
 * <li>{@link TcpAioSession#getAttachment()} </li>
 * <li>{@link TcpAioSession#getInputStream()} </li>
 * <li>{@link TcpAioSession#getInputStream(int)} </li>
 * <li>{@link TcpAioSession#getLocalAddress()} </li>
 * <li>{@link TcpAioSession#getRemoteAddress()} </li>
 * <li>{@link TcpAioSession#getSessionID()} </li>
 * <li>{@link TcpAioSession#isInvalid()} </li>
 * <li>{@link TcpAioSession#setAttachment(Object)}  </li>
 * </ol>
 *
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 */
class TcpAioSession<T> extends AioSession<T> {

    /**
     * 底层通信channel对象
     */
    protected AsynchronousSocketChannel channel;
    /**
     * 读缓冲。
     * <p>大小取决于AioQuickClient/AioQuickServer设置的setReadBufferSize</p>
     */
    protected VirtualBuffer readBuffer;
    /**
     * 写缓冲
     */
    protected VirtualBuffer writeBuffer;
    /**
     * read递归回调标识
     */
    private AtomicReference<Thread> threadReference = null;

    /**
     * 输出信号量,防止并发write导致异常
     */
    private Semaphore semaphore = new Semaphore(1);
    /**
     * 读回调
     */
    private ReadCompletionHandler<T> readCompletionHandler;
    /**
     * 写回调
     */
    private WriteCompletionHandler<T> writeCompletionHandler;
    /**
     * 服务配置
     */
    private IoServerConfig<T> ioServerConfig;
    /**
     * 同步输入流
     */
    private InputStream inputStream;
    /**
     * 输出流
     */
    private WriteBuffer byteBuf;
    /**
     * 是否处于数据输出中
     */
    private boolean writing = false;
    /**
     * 最近一次读取到的字节数
     */
    private int lastReadSize;

    /**
     * 数据输出Function
     */
    private Function<WriteBuffer, Void> flushFunction = new Function<WriteBuffer, Void>() {
        @Override
        public Void apply(WriteBuffer var) {
            if (!semaphore.tryAcquire()) {
                return null;
            }
            TcpAioSession.this.writeBuffer = var.poll();
            if (writeBuffer == null) {
                semaphore.release();
            } else {
                writing = true;
                continueWrite(writeBuffer);
            }
            return null;
        }
    };

    /**
     * @param channel
     * @param config
     * @param readCompletionHandler
     * @param writeCompletionHandler
     * @param bufferPage             是否服务端Session
     */
    TcpAioSession(AsynchronousSocketChannel channel, final IoServerConfig<T> config, ReadCompletionHandler<T> readCompletionHandler, WriteCompletionHandler<T> writeCompletionHandler, BufferPage bufferPage) {
        this.channel = channel;
        this.readCompletionHandler = readCompletionHandler;
        this.writeCompletionHandler = writeCompletionHandler;
        this.ioServerConfig = config;

        this.readBuffer = bufferPage.allocate(config.getReadBufferSize());
        byteBuf = new WriteBuffer(bufferPage, flushFunction, ioServerConfig);
        //触发状态机
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
    }


    /**
     * 初始化AioSession
     */
    void initSession() {
        continueRead();
    }

    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeToChannel() {
        if (writeBuffer == null) {
            writeBuffer = byteBuf.poll();
        } else if (!writeBuffer.buffer().hasRemaining()) {
            writeBuffer.clean();
            writeBuffer = byteBuf.poll();
        }

        if (writeBuffer != null) {
            continueWrite(writeBuffer);
            return;
        }
        writing = false;
        semaphore.release();
        //此时可能是Closing或Closed状态
        if (status != SESSION_STATUS_ENABLED) {
            close();
        } else {
            //也许此时有新的消息通过write方法添加到writeCacheQueue中
            byteBuf.flush();
        }
    }

    AtomicReference<Thread> getThreadReference() {
        return threadReference;
    }

    void setThreadReference(AtomicReference<Thread> threadReference) {
        this.threadReference = threadReference;
    }

    /**
     * 内部方法：触发通道的读操作
     *
     * @param buffer 用于存放待读取数据的buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, readCompletionHandler);
    }

    /**
     * 内部方法：触发通道的写操作
     *
     * @param buffer 待输出的buffer
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        channel.write(buffer, 0L, TimeUnit.MILLISECONDS, this, writeCompletionHandler);
    }

    /**
     * @return 输入流
     */
    public final WriteBuffer writeBuffer() {
        return byteBuf;
    }

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public synchronized void close(boolean immediate) {
        //status == SESSION_STATUS_CLOSED说明close方法被重复调用
        if (status == SESSION_STATUS_CLOSED) {
            System.out.println("ignore, session:" + getSessionID() + " is closed:");
            return;
        }
        status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
        if (immediate) {
            byteBuf.close();
            byteBuf = null;
            readBuffer.clean();
            readBuffer = null;
            if (writeBuffer != null) {
                writeBuffer.clean();
                writeBuffer = null;
            }
            IOUtil.close(channel);
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
        } else if ((writeBuffer == null || !writeBuffer.buffer().hasRemaining()) && !byteBuf.hasData()) {
            close(true);
        } else {
            ioServerConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
            byteBuf.flush();
        }
    }

    /**
     * 获取当前Session的唯一标识
     *
     * @return sessionId
     */
    public final String getSessionID() {
        return "aioSession-" + hashCode();
    }

    /**
     * 当前会话是否已失效
     *
     * @return 是否失效
     */
    public final boolean isInvalid() {
        return status != SESSION_STATUS_ENABLED;
    }


    /**
     * 触发通道的读回调操作
     *
     * @param eof 输入流是否已关闭
     */
    void readFromChannel(boolean eof) {
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }
        final ByteBuffer readBuffer = this.readBuffer.buffer();
        readBuffer.flip();
        final MessageProcessor<T> messageProcessor = ioServerConfig.getProcessor();
        while (readBuffer.hasRemaining() && status == SESSION_STATUS_ENABLED) {
            T dataEntry = null;
            try {
                dataEntry = ioServerConfig.getProtocol().decode(readBuffer, this);
            } catch (Exception e) {
                messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, e);
                throw e;
            }
            if (dataEntry == null) {
                break;
            }

            //处理消息
            try {
                messageProcessor.process(this, dataEntry);
            } catch (Exception e) {
                messageProcessor.stateEvent(this, StateMachineEnum.PROCESS_EXCEPTION, e);
            }
        }

        if (eof || status == SESSION_STATUS_CLOSING) {
            close(false);
            messageProcessor.stateEvent(this, StateMachineEnum.INPUT_SHUTDOWN, null);
            return;
        }
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }

        if (!writing && byteBuf != null) {
            byteBuf.flush();
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

        //读缓冲区已满
        if (!readBuffer.hasRemaining()) {
            RuntimeException exception = new RuntimeException("readBuffer overflow");
            messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, exception);
            throw exception;
        }

        continueRead();
    }

    /**
     * 触发读操作
     */
    protected void continueRead() {
        NetMonitor<T> monitor = getServerConfig().getMonitor();
        if (monitor != null) {
            monitor.beforeRead(this);
        }
        readFromChannel0(readBuffer.buffer());
    }

    /**
     * 同步读取数据
     *
     * @return
     * @throws Exception
     */
    private int synRead() throws IOException {
        ByteBuffer buffer = readBuffer.buffer();
        if (buffer.remaining() > 0) {
            return 0;
        }
        try {
            buffer.clear();
            int size = channel.read(buffer).get();
            buffer.flip();
            return size;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * 触发写操作
     *
     * @param writeBuffer 存放待输出数据的buffer
     */
    protected void continueWrite(VirtualBuffer writeBuffer) {
        NetMonitor<T> monitor = getServerConfig().getMonitor();
        if (monitor != null) {
            monitor.beforeWrite(this);
        }
        writeToChannel0(writeBuffer.buffer());
    }

    int getLastReadSize() {
        return lastReadSize;
    }

    void setLastReadSize(int lastReadSize) {
        this.lastReadSize = lastReadSize;
    }

    /**
     * @return 本地地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public final InetSocketAddress getLocalAddress() throws IOException {
        assertChannel();
        return (InetSocketAddress) channel.getLocalAddress();
    }

    /**
     * @return 远程地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public final InetSocketAddress getRemoteAddress() throws IOException {
        assertChannel();
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    /**
     * 断言当前会话是否可用
     *
     * @throws IOException IO异常
     */
    private void assertChannel() throws IOException {
        if (status == SESSION_STATUS_CLOSED || channel == null) {
            throw new IOException("session is closed");
        }
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
     *
     * @return 同步读操作的流对象
     * @throws IOException io异常
     */
    public final InputStream getInputStream() throws IOException {
        return inputStream == null ? getInputStream(-1) : inputStream;
    }

    /**
     * 获取已知长度的InputStream
     *
     * @param length InputStream长度
     * @return 同步读操作的流对象
     * @throws IOException io异常
     */
    public final InputStream getInputStream(int length) throws IOException {
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
     * 同步读操作的InputStream
     */
    private class InnerInputStream extends InputStream {
        /**
         * 当前InputSteam可读字节数
         */
        private int remainLength;

        InnerInputStream(int length) {
            this.remainLength = length >= 0 ? length : -1;
        }

        @Override
        public int read() throws IOException {
            if (remainLength == 0) {
                return -1;
            }
            ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
            if (readBuffer.hasRemaining()) {
                remainLength--;
                return readBuffer.get();
            }
            if (synRead() == -1) {
                remainLength = 0;
            }
            return read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (remainLength == 0) {
                return -1;
            }
            if (remainLength > 0 && remainLength < len) {
                len = remainLength;
            }
            ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
            int size = 0;
            while (len > 0 && synRead() != -1) {
                int readSize = readBuffer.remaining() < len ? readBuffer.remaining() : len;
                readBuffer.get(b, off + size, readSize);
                size += readSize;
                len -= readSize;
            }
            remainLength -= size;
            return size;
        }

        @Override
        public int available() throws IOException {
            if (remainLength == 0) {
                return 0;
            }
            if (synRead() == -1) {
                remainLength = 0;
                return remainLength;
            }
            ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
            if (remainLength < -1) {
                return readBuffer.remaining();
            } else {
                return remainLength > readBuffer.remaining() ? readBuffer.remaining() : remainLength;
            }
        }

        @Override
        public void close() throws IOException {
            if (TcpAioSession.this.inputStream == InnerInputStream.this) {
                TcpAioSession.this.inputStream = null;
            }
        }
    }
}
