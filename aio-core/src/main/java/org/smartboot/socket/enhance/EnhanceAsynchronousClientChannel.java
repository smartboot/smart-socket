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
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;

/**
 * 模拟JDK7的AIO处理方式
 *
 * @author 三刀
 * @version V1.0 , 2018/5/24
 */
final class EnhanceAsynchronousClientChannel extends EnhanceAsynchronousServerChannel {

    /**
     * 处理当前连接IO事件的资源组
     */
    private final EnhanceAsynchronousChannelGroup group;

    public EnhanceAsynchronousClientChannel(EnhanceAsynchronousChannelGroup group, SocketChannel channel, boolean lowMemory) throws IOException {
        super(group, channel, lowMemory);
        this.group = group;
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        if (group.isTerminated()) {
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

    @Override
    public Future<Void> connect(SocketAddress remote) {
        FutureCompletionHandler<Void, Void> connectFuture = new FutureCompletionHandler<>();
        connect(remote, null, connectFuture);
        return connectFuture;
    }


    public <A> void doConnect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> completionHandler) {
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
                group.commonWorker.addRegister(selector -> {
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

}
