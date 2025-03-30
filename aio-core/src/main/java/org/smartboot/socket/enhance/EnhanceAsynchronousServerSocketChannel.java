/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EnhanceAsynchronousServerSocketChannel.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 该类实现了异步服务器Socket通道，用于处理客户端连接请求。
 * 主要功能包括：
 * 1. 监听并接受客户端的连接请求
 * 2. 支持异步接受连接操作
 * 3. 管理服务器Socket的生命周期
 * 4. 提供回调机制处理连接事件
 * 
 * @author 三刀
 * @version V1.0 , 2020/5/25
 */
final class EnhanceAsynchronousServerSocketChannel extends AsynchronousServerSocketChannel {
    /**
     * 底层的服务器Socket通道，用于实际的网络IO操作
     */
    private final ServerSocketChannel serverSocketChannel;
    
    /**
     * 异步通道组，用于管理通道的线程资源和事件分发
     */
    private final EnhanceAsynchronousChannelGroup enhanceAsynchronousChannelGroup;
    
    /**
     * 接受连接的回调处理器，用于处理新连接建立后的回调逻辑
     */
    private CompletionHandler<AsynchronousSocketChannel, Object> acceptCompletionHandler;
    
    /**
     * 用于Future方式调用时的回调处理器
     */
    private FutureCompletionHandler<AsynchronousSocketChannel, Void> acceptFuture;
    
    /**
     * 接受连接操作的附加对象，可在回调时传递额外的上下文信息
     */
    private Object attachment;
    
    /**
     * 用于接受连接操作的选择键，管理通道的接受事件注册
     */
    private SelectionKey selectionKey;
    
    /**
     * 标识是否有待处理的接受连接操作
     */
    private boolean acceptPending;
    
    /**
     * 是否启用低内存模式
     * 在低内存模式下，会采用特殊的内存管理策略以减少内存占用
     */
    private final boolean lowMemory;

    /**
     * 接受连接操作的调用计数器
     * 用于限制连续接受连接的次数，避免某个服务器持续占用接受线程
     */
    private int acceptInvoker;

    EnhanceAsynchronousServerSocketChannel(EnhanceAsynchronousChannelGroup enhanceAsynchronousChannelGroup, boolean lowMemory) throws IOException {
        super(enhanceAsynchronousChannelGroup.provider());
        this.enhanceAsynchronousChannelGroup = enhanceAsynchronousChannelGroup;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        this.lowMemory = lowMemory;
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        serverSocketChannel.bind(local, backlog);
        return this;
    }

    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        serverSocketChannel.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return serverSocketChannel.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return serverSocketChannel.supportedOptions();
    }

    @Override
    public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        if (acceptPending) {
            throw new AcceptPendingException();
        }
        acceptPending = true;
        this.acceptCompletionHandler = (CompletionHandler<AsynchronousSocketChannel, Object>) handler;
        this.attachment = attachment;
        doAccept();
    }

    public void doAccept() {
        try {
            //此前通过Future调用,且触发了cancel
            if (acceptFuture != null && acceptFuture.isDone()) {
                resetAccept();
                EnhanceAsynchronousChannelGroup.removeOps(selectionKey, SelectionKey.OP_ACCEPT);
                return;
            }
            SocketChannel socketChannel = null;
            if (acceptInvoker++ < EnhanceAsynchronousChannelGroup.MAX_INVOKER) {
                socketChannel = serverSocketChannel.accept();
            }
            if (socketChannel != null) {
                EnhanceAsynchronousSocketChannel asynchronousSocketChannel = new EnhanceAsynchronousSocketChannel(enhanceAsynchronousChannelGroup, socketChannel, lowMemory);
                //这行代码不要乱动
                socketChannel.configureBlocking(false);
                socketChannel.finishConnect();
                CompletionHandler<AsynchronousSocketChannel, Object> completionHandler = acceptCompletionHandler;
                Object attach = attachment;
                resetAccept();
                completionHandler.completed(asynchronousSocketChannel, attach);
                if (!acceptPending && selectionKey != null) {
                    EnhanceAsynchronousChannelGroup.removeOps(selectionKey, SelectionKey.OP_ACCEPT);
                }
            }
            //首次注册selector
            else if (selectionKey == null) {
                enhanceAsynchronousChannelGroup.commonWorker.addRegister(selector -> {
                    try {
                        selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, EnhanceAsynchronousServerSocketChannel.this);
//                        selectionKey.attach(EnhanceAsynchronousServerSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        acceptCompletionHandler.failed(e, attachment);
                    }
                });
            } else {
                EnhanceAsynchronousChannelGroup.interestOps(enhanceAsynchronousChannelGroup.commonWorker, selectionKey, SelectionKey.OP_ACCEPT);
            }
        } catch (IOException e) {
            this.acceptCompletionHandler.failed(e, attachment);
        } finally {
            acceptInvoker = 0;
        }
    }

    private void resetAccept() {
        acceptPending = false;
        acceptFuture = null;
        acceptCompletionHandler = null;
        attachment = null;
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        FutureCompletionHandler<AsynchronousSocketChannel, Void> acceptFuture = new FutureCompletionHandler<>();
        accept(null, acceptFuture);
        this.acceptFuture = acceptFuture;
        return acceptFuture;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return serverSocketChannel.getLocalAddress();
    }

    @Override
    public boolean isOpen() {
        return serverSocketChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        serverSocketChannel.close();
    }
}
