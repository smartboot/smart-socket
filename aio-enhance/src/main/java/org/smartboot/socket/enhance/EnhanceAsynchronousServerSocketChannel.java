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
 * @author 三刀
 * @version V1.0 , 2020/5/25
 */
final class EnhanceAsynchronousServerSocketChannel extends AsynchronousServerSocketChannel {
    private final ServerSocketChannel serverSocketChannel;
    private final EnhanceAsynchronousChannelGroup enhanceAsynchronousChannelGroup;
    private final EnhanceAsynchronousChannelGroup.Worker acceptWorker;
    private CompletionHandler<AsynchronousSocketChannel, Object> acceptCompletionHandler;
    private FutureCompletionHandler<AsynchronousSocketChannel, Void> acceptFuture;
    private Object attachment;
    private SelectionKey selectionKey;
    private boolean acceptPending;
    private final boolean lowMemory;

    EnhanceAsynchronousServerSocketChannel(EnhanceAsynchronousChannelGroup enhanceAsynchronousChannelGroup, boolean lowMemory) throws IOException {
        super(enhanceAsynchronousChannelGroup.provider());
        this.enhanceAsynchronousChannelGroup = enhanceAsynchronousChannelGroup;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        acceptWorker = enhanceAsynchronousChannelGroup.getCommonWorker();
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
                enhanceAsynchronousChannelGroup.removeOps(selectionKey, SelectionKey.OP_ACCEPT);
                return;
            }
            boolean directAccept = (acceptWorker.getWorkerThread() == Thread.currentThread()
                    && acceptWorker.invoker++ < EnhanceAsynchronousChannelGroup.MAX_INVOKER);
            SocketChannel socketChannel = null;
            if (directAccept) {
                socketChannel = serverSocketChannel.accept();
            }
            if (socketChannel != null) {
                EnhanceAsynchronousSocketChannel asynchronousSocketChannel = new EnhanceAsynchronousSocketChannel(enhanceAsynchronousChannelGroup, socketChannel, lowMemory);
                socketChannel.configureBlocking(false);
                socketChannel.finishConnect();
                CompletionHandler<AsynchronousSocketChannel, Object> completionHandler = acceptCompletionHandler;
                Object attach = attachment;
                resetAccept();
                completionHandler.completed(asynchronousSocketChannel, attach);
                if (!acceptPending && selectionKey != null) {
                    enhanceAsynchronousChannelGroup.removeOps(selectionKey, SelectionKey.OP_ACCEPT);
                }
            }
            //首次注册selector
            else if (selectionKey == null) {
                acceptWorker.addRegister(selector -> {
                    try {
                        selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, EnhanceAsynchronousServerSocketChannel.this);
//                        selectionKey.attach(EnhanceAsynchronousServerSocketChannel.this);
                    } catch (ClosedChannelException e) {
                        acceptCompletionHandler.failed(e, attachment);
                    }
                });
            } else {
                enhanceAsynchronousChannelGroup.interestOps(acceptWorker, selectionKey, SelectionKey.OP_ACCEPT);
            }
        } catch (IOException e) {
            this.acceptCompletionHandler.failed(e, attachment);
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
