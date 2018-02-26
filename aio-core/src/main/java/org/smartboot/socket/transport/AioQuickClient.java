/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickClient.java
 * Date: 2017-11-25
 * Author: sandao
 */


package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * AIO实现的客户端服务
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickClient<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickClient.class);
    protected AsynchronousSocketChannel socketChannel = null;
    /**
     * IO事件处理线程组
     */
    protected AsynchronousChannelGroup asynchronousChannelGroup;
    /**
     * 客户端服务配置
     */
    protected IoServerConfig<T> config = new IoServerConfig<T>();

    public AioQuickClient() {
    }

    /**
     * @param host             远程服务器地址
     * @param port             远程服务器端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickClient(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        connect(host, port).setProtocol(protocol).setProcessor(messageProcessor);
    }

    /**
     * @param asynchronousChannelGroup
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        AioSession session = new AioSession<T>(socketChannel, config, new ReadCompletionHandler(), new WriteCompletionHandler(), false);
        session.initSession();
    }

    /**
     * 启动客户端Socket服务
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public final void start() throws IOException, ExecutionException, InterruptedException {
        this.asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        start(asynchronousChannelGroup);
    }

    /**
     * 停止客户端服务
     */
    public final void shutdown() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (Exception e) {
                LOGGER.catching(e);
            }
        }
        //仅Client内部创建的ChannelGroup需要shutdown
        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
        }
    }

    /**
     * 设置远程连接的地址、端口
     *
     * @param host
     * @param port
     * @return
     */
    public final AioQuickClient<T> connect(String host, int port) {
        this.config.setHost(host);
        this.config.setPort(port);
        return this;
    }

    /**
     * 设置协议对象
     *
     * @param protocol
     * @return
     */
    public final AioQuickClient<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public final AioQuickClient<T> setFilters(Filter<T>[] filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public final AioQuickClient<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size
     * @return
     */
    public final AioQuickClient<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     * @return
     */
    public final AioQuickClient<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }
}
