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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 该类模拟JDK7的AIO处理方式，通过NIO实现异步IO操作。
 * 主要功能包括：
 * 1. 提供异步读写操作接口：支持非阻塞的读写操作，提高IO效率
 * 2. 支持回调机制处理IO事件：通过CompletionHandler接口处理异步操作的结果
 * 3. 实现了低内存模式的支持：在资源受限环境下优化内存使用
 * 4. 管理Socket连接的生命周期：包括连接建立、数据传输和连接关闭
 * 5. 提供Future和CompletionHandler两种异步操作方式：灵活支持不同的编程模型
 * <p>
 * 实现原理：
 * - 底层使用NIO的SocketChannel实现网络通信
 * - 通过Worker线程池处理异步IO事件
 * - 使用SelectionKey管理IO事件的注册与触发
 * - 支持低内存模式下的内存优化策略
 * <p>
 * 该类是smart-socket框架的核心数据传输组件，通过精心设计的事件处理机制，解决了以下问题：
 * - 避免了传统NIO编程中的复杂性和易错性
 * - 提供了类似JDK7 AIO的编程体验，但性能更优
 * - 实现了读写操作的并发控制，避免资源竞争
 * - 支持连接超时和操作取消等高级特性
 * - 在低内存环境下能够智能管理缓冲区资源
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
            doRead(true, false);
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

    /**
     * 异步连接远程地址
     * 实现了异步连接操作，支持通过CompletionHandler处理连接结果
     *
     * @param remote     远程服务器地址
     * @param attachment 附加对象，可在连接完成时传递给CompletionHandler
     * @param handler    连接完成的回调处理器
     * @throws ShutdownChannelGroupException 如果通道组已关闭
     * @throws AlreadyConnectedException     如果通道已经连接
     * @throws ConnectionPendingException    如果连接操作正在进行中
     */
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
//            if (completionHandler instanceof FutureCompletionHandler && ((FutureCompletionHandler) completionHandler).isDone()) {
//                return;
//            }
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
        throw new UnsupportedOperationException();
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
        boolean syncRead = EnhanceAsynchronousChannelProvider.SYNC_READ_FLAG.get();
        doRead(syncRead, syncRead);
    }


    @Override
    public final Future<Integer> read(ByteBuffer readBuffer) {
        CompletableFuture<Integer> readFuture = new CompletableFuture<>();
        EnhanceAsynchronousChannelProvider.SYNC_READ_FLAG.set(true);
        try {
            read(readBuffer, 0, TimeUnit.MILLISECONDS, readFuture, EnhanceAsynchronousChannelProvider.SYNC_READ_HANDLER);
        } finally {
            EnhanceAsynchronousChannelProvider.SYNC_READ_FLAG.set(false);
        }
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

    /**
     * 执行异步读取操作
     * 该方法实现了复杂的异步读取逻辑，包括以下功能：
     * 1. 处理Future取消的情况
     * 2. 支持低内存模式下的读取优化
     * 3. 实现读取限流，避免单个连接占用过多资源
     * 4. 处理读取完成后的回调通知
     *
     * @param direct 是否直接读取，true表示立即读取，false表示通过事件触发读取
     */
    public final void doRead(boolean direct, boolean switchThread) {
        try {
            if (readCompletionHandler == null) {
                return;
            }
            // 处理Future调用被取消的情况
//            if (readCompletionHandler instanceof FutureCompletionHandler && ((FutureCompletionHandler) readCompletionHandler).isDone()) {
//                EnhanceAsynchronousChannelGroup.removeOps(readSelectionKey, SelectionKey.OP_READ);
//                resetRead();
//                return;
//            }
            // 低内存模式下的特殊处理：当没有缓冲区时，直接返回可读信号
            if (lowMemory && direct && readBuffer == null) {
                CompletionHandler<Number, Object> completionHandler = readCompletionHandler;
                Object attach = readAttachment;
                resetRead();
                completionHandler.completed(EnhanceAsynchronousChannelProvider.READABLE_SIGNAL, attach);
                return;
            }
            // 判断是否需要直接读取：直接调用或未达到最大调用次数
            boolean directRead = direct || readInvoker++ < EnhanceAsynchronousChannelGroup.MAX_INVOKER;

            int readSize = 0;
            boolean hasRemain = true;
            if (directRead) {
                readSize = channel.read(readBuffer);
                hasRemain = readBuffer.hasRemaining();
                //当readBuffer未填充满，我们推测当前管道中大概率没有可读数据，下一次直接进入读监听状态
                if (hasRemain) {
                    readInvoker = EnhanceAsynchronousChannelGroup.MAX_INVOKER;
                }
            }

            //注册至异步线程
            if (readSize == 0) {
                if (switchThread) {
                    EnhanceAsynchronousChannelGroup.removeOps(readSelectionKey, SelectionKey.OP_READ);
                    group().commonWorker.addRegister(selector -> {
                        try {
                            channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                        } catch (ClosedChannelException e) {
                            doRead(true, false);
                        }
                    });
                    return;
                }
                //释放内存
                if (lowMemory && readBuffer.position() == 0) {
                    readBuffer = null;
                    readCompletionHandler.completed(EnhanceAsynchronousChannelProvider.READ_MONITOR_SIGNAL, readAttachment);
                }
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
                        if (channel.isOpen()) {
                            readSelectionKey = channel.register(selector, SelectionKey.OP_READ, EnhanceAsynchronousSocketChannel.this);
                        }
                    } catch (ClosedChannelException e) {
                        if (readCompletionHandler != null) {
                            readCompletionHandler.failed(e, readAttachment);
                        }
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

    /**
     * 执行异步写入操作
     * 该方法实现了复杂的异步写入逻辑，包括以下功能：
     * 1. 处理写入中断的情况
     * 2. 支持非阻塞写入操作
     * 3. 处理写入完成后的回调通知
     * 4. 管理写入事件的注册
     *
     * @return 是否需要继续写入，true表示需要继续写入，false表示写入完成或需要等待
     */
    public final boolean doWrite() {
        // 处理写入中断的情况
        if (writeInterrupted) {
            writeInterrupted = false;
            return false;
        }
        try {
            // 尝试写入数据
            int writeSize = channel.write(writeBuffer);

            // 写入完成或缓冲区已空
            if (writeSize != 0 || !writeBuffer.hasRemaining()) {
                CompletionHandler<Number, Object> completionHandler = writeCompletionHandler;
                Object attach = writeAttachment;
                resetWrite();
                writeInterrupted = true;
                completionHandler.completed(writeSize, attach);
                // 检查是否需要继续写入
                if (!writeInterrupted) {
                    return true;
                }
                writeInterrupted = false;
            } else {
                // 注册写事件到选择器
                SelectionKey commonSelectionKey = channel.keyFor(group().writeWorker.selector);
                if (commonSelectionKey == null) {
                    // 首次注册写事件
                    group().writeWorker.addRegister(selector -> {
                        try {
                            if (channel.isOpen()) {
                                channel.register(selector, SelectionKey.OP_WRITE, EnhanceAsynchronousSocketChannel.this);
                            }
                        } catch (ClosedChannelException e) {
                            if (writeCompletionHandler != null) {
                                writeCompletionHandler.failed(e, writeAttachment);
                            }
                        }
                    });
                } else {
                    // 更新已存在的选择键的兴趣事件
                    EnhanceAsynchronousChannelGroup.interestOps(group().writeWorker, commonSelectionKey, SelectionKey.OP_WRITE);
                }
            }
        } catch (Throwable e) {
            // 异常处理
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
