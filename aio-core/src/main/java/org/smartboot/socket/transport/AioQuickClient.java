/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickClient.java
 * Date: 2017-11-25
 * Author: sandao
 */


package org.smartboot.socket.transport;

import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * AIO实现的客户端服务
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickClient<T> {
    /**
     * IO事件处理线程组
     */
    protected AsynchronousChannelGroup asynchronousChannelGroup;
    /**
     * 客户端服务配置
     */
    protected IoServerConfig<T> config = new IoServerConfig<>();

    protected AioSession session;

    /**
     * @param host             远程服务器地址
     * @param port             远程服务器端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickClient(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setHost(host);
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * @param asynchronousChannelGroup
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        //set socket options
        if (config.getSocketOptions() != null) {
            for (Map.Entry<SocketOption<Object>, Object> entry : config.getSocketOptions().entrySet()) {
                socketChannel.setOption(entry.getKey(), entry.getValue());
            }
        }
        //bind host
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        session = new AioSession<T>(socketChannel, config, new ReadCompletionHandler(), new WriteCompletionHandler(), false);
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
        if (session != null) {
            session.close();
            session = null;
        }
        //仅Client内部创建的ChannelGroup需要shutdown
        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
        }
    }


    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     */
    public final AioQuickClient<T> setFilters(Filter<T>[] filters) {
        this.config.setFilters(filters);
        return this;
    }


    /**
     * 设置读缓存区大小
     *
     * @param size
     */
    public final AioQuickClient<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     */
    public final AioQuickClient<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 是否启用DirectByteBuffer
     *
     * @param directBuffer
     */
    public final AioQuickClient<T> setDirectBuffer(boolean directBuffer) {
        config.setDirectBuffer(directBuffer);
        return this;
    }

    /**
     *
     * @param socketOption
     * @param value
     * @param <V>
     * @return
     */
    public final <V> AioQuickClient<T> setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }
}
