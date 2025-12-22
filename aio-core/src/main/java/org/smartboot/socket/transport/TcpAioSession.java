/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TcpAioSession.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;


import org.smartboot.socket.DecoderException;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
 * <li>{@link TcpAioSession#getLocalAddress()} </li>
 * <li>{@link TcpAioSession#getRemoteAddress()} </li>
 * <li>{@link TcpAioSession#isInvalid()} </li>
 * <li>{@link TcpAioSession#setAttachment(Object)}  </li>
 * </ol>
 *
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 */
final class TcpAioSession extends AioSession {
    /**
     * 底层通信channel对象
     */
    private final AsynchronousSocketChannel channel;
    /**
     * 读事件回调处理
     */
    private static final CompletionHandler<Integer, TcpAioSession> READ_COMPLETION_HANDLER = new CompletionHandler<Integer, TcpAioSession>() {
        @Override
        public void completed(Integer result, TcpAioSession aioSession) {
            try {
                aioSession.readCompleted(result);
            } catch (Throwable throwable) {
                failed(throwable, aioSession);
            }
        }

        @Override
        public void failed(Throwable exc, TcpAioSession aioSession) {
            try {
                aioSession.config.getProcessor().stateEvent(aioSession, StateMachineEnum.INPUT_EXCEPTION, exc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                aioSession.close(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * 输出流
     */
    private final WriteBufferImpl byteBuf;
    /**
     * 写事件回调处理
     */
    private static final CompletionHandler<Integer, TcpAioSession> WRITE_COMPLETION_HANDLER = new CompletionHandler<Integer, TcpAioSession>() {
        @Override
        public void completed(Integer result, TcpAioSession aioSession) {
            try {
                aioSession.writeCompleted(result);
            } catch (Throwable throwable) {
                failed(throwable, aioSession);
            }
        }

        @Override
        public void failed(Throwable exc, TcpAioSession aioSession) {
            try {
                aioSession.config.getProcessor().stateEvent(aioSession, StateMachineEnum.OUTPUT_EXCEPTION, exc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                aioSession.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private final IoServerConfig config;
    private final Supplier<VirtualBuffer> readBufferSupplier;
    /**
     * 读缓冲。
     * <p>大小取决于AioQuickClient/AioQuickServer设置的setReadBufferSize</p>
     */
    private VirtualBuffer readBuffer;
    /**
     * 写缓冲
     */
    private VirtualBuffer writeBuffer;


    /**
     * @param channel Socket通道
     */
    TcpAioSession(AsynchronousSocketChannel channel, IoServerConfig config, BufferPagePool writeBufferPage, Supplier<VirtualBuffer> readBufferSupplier) {
        this.channel = channel;
        this.config = config;
        this.readBufferSupplier = readBufferSupplier;
        byteBuf = new WriteBufferImpl(writeBufferPage, this::continueWrite, config.getWriteChunkSize(), config.getWriteChunkCount());
        //触发状态机
        config.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
        doRead();
    }

    void doRead() {
        this.readBuffer = readBufferSupplier.get();
        this.readBuffer.buffer().flip();
        signalRead();
    }

    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeCompleted(int result) {
        NetMonitor monitor = config.getMonitor();
        if (monitor != null) {
            monitor.afterWrite(this, result);
        }
        VirtualBuffer writeBuffer = TcpAioSession.this.writeBuffer;
        TcpAioSession.this.writeBuffer = null;
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
        byteBuf.finishWrite();
        //此时可能是Closing或Closed状态
        if (status != SESSION_STATUS_ENABLED) {
            close();
        } else {
            byteBuf.flush();
        }
    }

    /**
     * @return 输入流
     */
    public WriteBuffer writeBuffer() {
        return byteBuf;
    }

    @Override
    public ByteBuffer readBuffer() {
        return readBuffer.buffer();
    }

    @Override
    public void awaitRead() {
        modCount++;
    }

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public void close(boolean immediate) {
        synchronized (byteBuf) {
            //status == SESSION_STATUS_CLOSED说明close方法被重复调用
            if (status == SESSION_STATUS_CLOSED) {
//            System.out.println("ignore, session:" + getSessionID() + " is closed:");
                return;
            }
            status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
            if (immediate) {
                try {
                    byteBuf.close();
                    if (readBuffer != null) {
                        readBuffer.clean();
                        readBuffer = null;
                    }
                    if (writeBuffer != null) {
                        writeBuffer.clean();
                        writeBuffer = null;
                    }
                } finally {
                    IOUtil.close(channel);
                    config.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
                }
            } else if ((writeBuffer == null || !writeBuffer.buffer().hasRemaining()) && byteBuf.isEmpty()) {
                close(true);
            } else {
                config.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
                byteBuf.flush();
            }
        }
    }

    void readCompleted(int result) {
        //释放缓冲区
        if (result == EnhanceAsynchronousChannelProvider.READ_MONITOR_SIGNAL) {
            this.readBuffer.clean();
            this.readBuffer = null;
            return;
        }
        if (result == EnhanceAsynchronousChannelProvider.READABLE_SIGNAL) {
            doRead();
            return;
        }
        // 接收到的消息进行预处理
        NetMonitor monitor = config.getMonitor();
        if (monitor != null) {
            monitor.afterRead(this, result);
        }
        this.eof = result == -1;
        if (SESSION_STATUS_CLOSED != status) {
            this.readBuffer.buffer().flip();
            signalRead();
        }
    }

    /**
     * 触发通道的读回调操作
     */
    public void signalRead() {
        int modCount = this.modCount;
        if (status == SESSION_STATUS_CLOSED) {
            return;
        }
        final ByteBuffer readBuffer = this.readBuffer.buffer();
        final MessageProcessor messageProcessor = config.getProcessor();
        while (readBuffer.hasRemaining() && status == SESSION_STATUS_ENABLED) {
            Object dataEntry;
            try {
                dataEntry = config.getProtocol().decode(readBuffer, this);
            } catch (Throwable e) {
                messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, e);
                throw e;
            }
            if (dataEntry == null) {
                break;
            }

            //处理消息
            try {
                messageProcessor.process(this, dataEntry);
                // eof 情况下只提供一次解析机会，避免出现死循环
                if (eof) {
                    break;
                }
                if (modCount != this.modCount) {
                    return;
                }
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

        byteBuf.flush();

        if (readBuffer.hasRemaining()) {
            readBuffer.compact();
            //读缓冲区已满
            if (!readBuffer.hasRemaining()) {
                DecoderException exception = new DecoderException("readBuffer overflow. The current TCP connection " + "will be closed. Please fix your " + config.getProtocol().getClass().getSimpleName() + "#decode bug.");
                messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, exception);
                throw exception;
            }
        } else {
            readBuffer.clear();
        }

        //read from channel
        NetMonitor monitor = config.getMonitor();
        if (monitor != null) {
            monitor.beforeRead(this);
        }
        channel.read(readBuffer, 0L, TimeUnit.MILLISECONDS, this, READ_COMPLETION_HANDLER);
    }

    /**
     * 同步读取数据
     */
    public int read(long timeout, TimeUnit unit) throws IOException {
        ByteBuffer buffer = readBuffer.buffer();
        buffer.compact();
        int readSize;
        try {
            if (timeout <= 0) {
                readSize = channel.read(buffer).get();
            } else {
                readSize = channel.read(buffer).get(timeout, unit);
            }
            buffer.flip();
            return readSize;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * 触发写操作
     *
     * @param writeBuffer 存放待输出数据的buffer
     */
    private void continueWrite(VirtualBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
        NetMonitor monitor = config.getMonitor();
        if (monitor != null) {
            monitor.beforeWrite(this);
        }
        channel.write(writeBuffer.buffer(), 0L, TimeUnit.MILLISECONDS, this, WRITE_COMPLETION_HANDLER);
    }

    /**
     * @return 本地地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public InetSocketAddress getLocalAddress() throws IOException {
        assertChannel();
        return (InetSocketAddress) channel.getLocalAddress();
    }

    /**
     * @return 远程地址
     * @throws IOException IO异常
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public InetSocketAddress getRemoteAddress() throws IOException {
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


}
