/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EnhanceAsynchronousSocketChannel.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritePendingException;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 该类模拟JDK7的AIO处理方式，通过NIO实现异步IO操作。
 * 主要功能包括：
 * 1. 提供异步读写操作接口
 * 2. 支持回调机制处理IO事件
 * 3. 实现了低内存模式的支持
 * 4. 管理Socket连接的生命周期
 * 5. 提供Future和CompletionHandler两种异步操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/5/24
 */
class EnhanceAsynchronousSocketChannel extends AsynchronousSocketChannel {
    /**
     * 底层的Socket通道，用于实际的网络IO操作
     * 该通道是非阻塞模式的，支持异步读写操作
     */
    protected final SocketChannel channel;
    
    /**
     * 处理读事件的工作线程，负责异步读取操作的执行
     * 通过Worker线程池来处理读事件，实现真正的异步操作
     */
    private final EnhanceAsynchronousChannelGroup.Worker readWorker;

    /**
     * 读缓冲区，用于存储从通道读取的数据
     * 数据读取后经过解码处理，处理完成后缓冲区可重复使用
     * 采用ByteBuffer实现高效的数据读取和处理
     */
    private ByteBuffer readBuffer;
    
    /**
     * 写缓冲区，用于存储待写入通道的数据
     * 支持异步写入操作，提高IO效率
     */
    private ByteBuffer writeBuffer;

    /**
     * 读操作的回调处理器，用于处理异步读取完成后的回调逻辑
     * 支持自定义处理读取结果的方式
     */
    private CompletionHandler<Number, Object> readCompletionHandler;
    
    /**
     * 写操作的回调处理器，用于处理异步写入完成后的回调逻辑
     * 支持自定义处理写入结果的方式
     */
    private CompletionHandler<Number, Object> writeCompletionHandler;
    
    /**
     * 读操作的附加对象，可在回调时传递额外的上下文信息
     * 用于在异步操作完成时传递自定义数据
     */
    private Object readAttachment;
    
    /**
     * 写操作的附加对象，可在回调时传递额外的上下文信息
     * 用于在异步操作完成时传递自定义数据
     */
    private Object writeAttachment;
    
    /**
     * 用于读操作的选择键，管理通道的读事件注册
     * 通过SelectionKey实现事件的监听和触发
     */
    private SelectionKey readSelectionKey;

    /**
     * 是否启用低内存模式
     * 在低内存模式下，会采用特殊的内存管理策略以减少内存占用
     * 适用于资源受限的环境
     */
    private final boolean lowMemory;
    
    /**
     * 写操作中断标志
     * 用于控制写操作的中断状态，防止写操作重入
     * true表示写操作被中断，false表示可以继续写入
     */
    private boolean writeInterrupted;

    /**
     * 读操作调用计数器
     * 用于限制连续读取的次数，避免某个连接持续占用读取线程
     * 达到最大值后会暂停读取，等待下一次事件触发
     */
    private byte readInvoker = EnhanceAsynchronousChannelGroup.MAX_INVOKER;

    public EnhanceAsynchronousSocketChannel(EnhanceAsynchronousChannelGroup group, SocketChannel channel, boolean lowMemory) throws IOException {
        super(group.provider());
        this.channel = channel;
        readWorker = group.getReadWorker();
        this.lowMemory = lowMemory;
    }

    protected EnhanceAsynchronousChannelGroup group() {
        return readWorker.group();
    }

    @Override
    public final void close() throws IOException {
        IOException exception = null;
        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            exception = e;
        }
        if (readCompletionHandler != null) {
            doRead(true);
        }
        if (readSelectionKey != null) {
            readSelectionKey.cancel();
            readSelectionKey = null;
        }
        SelectionKey key = channel.keyFor(group().writeWorker.selector);
        if (key != null) {
            key.cancel();
        }
        key = channel.keyFor(group().commonWorker.selector);
        if (key != null) {
            key.cancel();
        }
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public final AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        channel.bind(local);
        return this;
    }

    @Override
    public final <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        channel.setOption(name, value);
        return this;
    }

    @Override
    public final <T> T getOption(SocketOption<T> name) throws IOException {
        return channel.getOption(name);
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return channel.supportedOptions();
    }

    @Override
    public final AsynchronousSocketChannel shutdownInput() throws IOException {
        channel.shutdownInput();
        return this;
    }

    @Override
    public final AsynchronousSocketChannel shutdownOutput() throws IOException {
        channel.shutdownOutput();
        return this;
    }

    @Override
    public final SocketAddress getRemoteAddress() throws IOException {
        return channel.getRemoteAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        if (group().isTerminated()) {
            throw new ShutdownChannelGroupException();
        }
        if (channel.isConnected()) {
            throw new AlreadyConnectedException();
        }
        if (channel.isConnectionPending()) {
            throw new ConnectionPendingException();
        }
        doConnect(remote, attachment, handler);
    }

    private <A> void doConnect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> completionHandler) {
        try {
            //此前通过Future调用,且触发了cancel
            if (completionHandler instanceof FutureCompletionHandler && ((FutureCompletionHandler) completionHandler).isDone()) {
                return;
            }
            boolean connected = channel.isConnectionPending();
            if (connected || channel.connect(remote)) {
                connected = channel.finishConnect();
            }
            //这行代码不要乱动
            channel.configureBlocking(false);
            if (connected) {
                completionHandler.completed(null, attachment);
            } else {
                group().commonWorker.addRegister(selector -> {
                    try {
                        channel.register(selector, SelectionKey.OP_CONNECT, (Runnable) () -> doConnect(remote, attachment, completionHandler));
                    } catch (ClosedChannelException e) {
                        completionHandler.failed(e, attachment);
                    }
                });
            }
        } catch (IOException e) {
            completionHandler.failed(e, attachment);
        }
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        FutureCompletionHandler<Void, Void> connectFuture = new FutureCompletionHandler<>();
        connect(remote, null, connectFuture);
        return connectFuture;
    }

    @Override
    public final <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (timeout > 0) {
            throw new UnsupportedOperationException();
        }
        read0(dst, attachment, handler);
    }

    private <V extends Number, A> void read0(ByteBuffer readBuffer, A attachment, CompletionHandler<V, ? super A> handler) {
        if (this.readCompletionHandler != null) {
            throw new ReadPendingException();
        }
        this.readBuffer = readBuffer;
        this.readAttachment = attachment;
        this.readCompletionHandler = (CompletionHandler<Number, Object>) handler;
        doRead(handler instanceof FutureCompletionHandler);
    }

    @Override
    public final Future<Integer> read(ByteBuffer readBuffer) {
        FutureCompletionHandler<Integer, Object> readFuture = new FutureCompletionHandler<>();
        read(readBuffer, 0, TimeUnit.MILLISECONDS, null, readFuture);
        return readFuture;
    }

    @Override
    public final <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (timeout > 0) {
            throw new UnsupportedOperationException();
        }
        write0(src, attachment, handler);
    }

    private <V extends Number, A> void write0(ByteBuffer writeBuffer, A attachment, CompletionHandler<V, ? super A> handler) {
        if (this.writeCompletionHandler != null) {
            throw new WritePendingException();
        }
        this.writeBuffer = writeBuffer;
        this.writeAttachment = attachment;
        this.writeCompletionHandler = (CompletionHandler<Number, Object>) handler;
        while (doWrite()) ;
    }

    @Override
    public final Future<Integer> write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final SocketAddress getLocalAddress() throws IOException {
        return channel.getLocalAddress();
    }

    public final void doRead(boolean direct) {
        try {
            if (readCompletionHandler == null) {
                return;
            }
            //此前通过Future调用,且触发了cancel
            if (readCompletionHandler instanceof FutureCompletionHandler && ((FutureCompletionHandler) readCompletionHandler).isDone()) {
                EnhanceAsynchronousChannelGroup.removeOps(readSelectionKey, SelectionKey.OP_READ);
                resetRead();
                return;
            }
            if (lowMemory && direct && readBuffer == null) {
                CompletionHandler<Number, Object> completionHandler = readCompletionHandler;
                Object attach = readAttachment;
                resetRead();
                completionHandler.completed(EnhanceAsynchronousChannelProvider.READABLE_SIGNAL, attach);
                return;
            }
            boolean directRead = direct || readInvoker++ < EnhanceAsynchronousChannelGroup.MAX_INVOKER;

            int readSize = 0;
            boolean hasRemain = true;
            if (directRead) {
                readSize = channel.read(readBuffer);
                hasRemain = readBuffer.hasRemaining();
            }

            //注册至异步线程
            if (readSize == 0 && readCompletionHandler instanceof FutureCompletionHandler) {
                EnhanceAsynchronousChannelGroup.removeOps(readSelectionKey, SelectionKey.OP_READ);
                group().commonWorker.addRegister(selector -> {
                    try {
                        channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        doRead(true);
                    }
                });
                return;
            }
            //释放内存
            if (lowMemory && readSize == 0 && readBuffer.position() == 0) {
                readBuffer = null;
                readCompletionHandler.completed(EnhanceAsynchronousChannelProvider.READ_MONITOR_SIGNAL, readAttachment);
            }

            if (readSize != 0 || !hasRemain) {
                CompletionHandler<Number, Object> completionHandler = readCompletionHandler;
                Object attach = readAttachment;
                resetRead();
                completionHandler.completed(readSize, attach);

                if (readCompletionHandler == null && readSelectionKey != null) {
                    EnhanceAsynchronousChannelGroup.removeOps(readSelectionKey, SelectionKey.OP_READ);
                }
            } else if (readSelectionKey == null) {
                readWorker.addRegister(selector -> {
                    try {
                        readSelectionKey = channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        readCompletionHandler.failed(e, readAttachment);
                    }
                });
            } else {
                EnhanceAsynchronousChannelGroup.interestOps(readWorker, readSelectionKey, SelectionKey.OP_READ);
            }
        } catch (Throwable e) {
            if (readCompletionHandler == null) {
                try {
                    close();
                } catch (Throwable ignore) {
                }
            } else {
                CompletionHandler<Number, Object> completionHandler = readCompletionHandler;
                Object attach = readAttachment;
                resetRead();
                completionHandler.failed(e, attach);
            }
        } finally {
            readInvoker = 0;
        }
    }

    private void resetRead() {
        readCompletionHandler = null;
        readAttachment = null;
        readBuffer = null;
    }

    public final boolean doWrite() {
        if (writeInterrupted) {
            writeInterrupted = false;
            return false;
        }
        try {
            int writeSize = channel.write(writeBuffer);

            if (writeSize != 0 || !writeBuffer.hasRemaining()) {
                CompletionHandler<Number, Object> completionHandler = writeCompletionHandler;
                Object attach = writeAttachment;
                resetWrite();
                writeInterrupted = true;
                completionHandler.completed(writeSize, attach);
                if (!writeInterrupted) {
                    return true;
                }
                writeInterrupted = false;
            } else {
                SelectionKey commonSelectionKey = channel.keyFor(group().writeWorker.selector);
                if (commonSelectionKey == null) {
                    group().writeWorker.addRegister(selector -> {
                        try {
                            channel.register(selector, SelectionKey.OP_WRITE, EnhanceAsynchronousSocketChannel.this);
                        } catch (ClosedChannelException e) {
                            writeCompletionHandler.failed(e, writeAttachment);
                        }
                    });
                } else {
                    EnhanceAsynchronousChannelGroup.interestOps(group().writeWorker, commonSelectionKey, SelectionKey.OP_WRITE);
                }
            }
        } catch (Throwable e) {
            if (writeCompletionHandler == null) {
                e.printStackTrace();
                try {
                    close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                writeCompletionHandler.failed(e, writeAttachment);
            }
        }
        return false;
    }

    private void resetWrite() {
        writeAttachment = null;
        writeCompletionHandler = null;
        writeBuffer = null;
    }

    @Override
    public final boolean isOpen() {
        return channel.isOpen();
    }
}
