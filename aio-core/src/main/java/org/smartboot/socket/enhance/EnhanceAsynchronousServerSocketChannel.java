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
 * 该类是服务器端网络编程的核心组件，通过非阻塞IO和事件通知机制，实现了高效的连接处理：
 * - 支持Future和CompletionHandler两种异步编程模式
 * - 实现了连接请求的排队和限流处理，避免服务器资源耗尽
 * - 提供了优雅的异常处理和资源管理机制
 * - 在低内存模式下采用特殊的资源管理策略
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

    /**
     * 构造函数
     * 创建一个新的增强型异步服务器Socket通道实例
     * 
     * @param enhanceAsynchronousChannelGroup 关联的异步通道组，用于管理该通道的资源
     * @param lowMemory 是否启用低内存模式
     * @throws IOException 如果创建底层通道时发生IO错误
     */
    EnhanceAsynchronousServerSocketChannel(EnhanceAsynchronousChannelGroup enhanceAsynchronousChannelGroup, boolean lowMemory) throws IOException {
        super(enhanceAsynchronousChannelGroup.provider());
        this.enhanceAsynchronousChannelGroup = enhanceAsynchronousChannelGroup;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        this.lowMemory = lowMemory;
    }

    /**
     * 将服务器Socket绑定到指定的本地地址
     * 
     * @param local 要绑定的本地地址
     * @param backlog 连接请求队列的最大长度
     * @return 返回当前服务器Socket通道实例
     * @throws IOException 如果绑定操作失败
     */
    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        serverSocketChannel.bind(local, backlog);
        return this;
    }

    /**
     * 设置服务器Socket的选项
     * 
     * @param name 选项名称
     * @param value 选项值
     * @return 返回当前服务器Socket通道实例
     * @throws IOException 如果设置选项时发生错误
     */
    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        serverSocketChannel.setOption(name, value);
        return this;
    }

    /**
     * 获取服务器Socket的选项值
     * 
     * @param name 选项名称
     * @return 返回指定选项的当前值
     * @throws IOException 如果获取选项值时发生错误
     */
    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return serverSocketChannel.getOption(name);
    }

    /**
     * 获取服务器Socket支持的所有选项
     * 
     * @return 返回支持的选项集合
     */
    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return serverSocketChannel.supportedOptions();
    }

    /**
     * 异步接受客户端连接请求
     * 该方法实现了异步接受连接的功能，通过回调机制通知连接建立的结果
     * 
     * @param attachment 附加对象，可在回调时获取
     * @param handler 连接完成的回调处理器
     * @throws AcceptPendingException 如果已有一个待处理的接受连接操作
     */
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

    /**
     * 执行接受连接操作
     * 该方法实现了实际的接受连接逻辑，包括：
     * 1. 处理Future取消的情况
     * 2. 尝试接受新的连接
     * 3. 处理接受到的连接
     * 4. 管理选择键的注册
     */
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

    /**
     * 重置接受连接操作的状态
     * 清除当前接受连接操作的相关状态，为下一次接受连接做准备
     */
    private void resetAccept() {
        acceptPending = false;
        acceptFuture = null;
        acceptCompletionHandler = null;
        attachment = null;
    }

    /**
     * 以Future方式接受连接
     * 提供基于Future的异步接受连接方式，允许调用者通过Future对象获取连接结果
     * 
     * @return 返回Future对象，可用于获取连接结果
     */
    @Override
    public Future<AsynchronousSocketChannel> accept() {
        FutureCompletionHandler<AsynchronousSocketChannel, Void> acceptFuture = new FutureCompletionHandler<>();
        accept(null, acceptFuture);
        this.acceptFuture = acceptFuture;
        return acceptFuture;
    }

    /**
     * 获取服务器Socket的本地地址
     * 
     * @return 返回服务器Socket绑定的本地地址
     * @throws IOException 如果获取地址时发生IO错误
     */
    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return serverSocketChannel.getLocalAddress();
    }

    /**
     * 检查服务器Socket通道是否打开
     * 
     * @return 如果通道处于打开状态返回true，否则返回false
     */
    @Override
    public boolean isOpen() {
        return serverSocketChannel.isOpen();
    }

    /**
     * 关闭服务器Socket通道
     * 关闭底层的服务器Socket通道，释放相关资源
     * 
     * @throws IOException 如果关闭时发生IO错误
     */
    @Override
    public void close() throws IOException {
        serverSocketChannel.close();
    }
}
