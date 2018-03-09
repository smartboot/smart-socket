/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
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
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * AIO服务端
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickServer.class);
    protected AsynchronousServerSocketChannel serverSocketChannel = null;
    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected IoServerConfig<T> config = new IoServerConfig<>();

    protected ReadCompletionHandler<T> aioReadCompletionHandler = new ReadCompletionHandler<>();
    protected WriteCompletionHandler<T> aioWriteCompletionHandler = new WriteCompletionHandler<>();

    public AioQuickServer() {
    }

    /**
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        bind(port).setProtocol(protocol).setProcessor(messageProcessor);
    }

    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            System.out.println(IoServerConfig.BANNER);
            System.out.println(" :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }
        start0();
    }

    protected final void start0() throws IOException {
        asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
            byte index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-socket:AIO-" + (++index));
            }
        });
        this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress(config.getPort()), 1000);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                serverSocketChannel.accept(attachment, this);
                createSession(channel);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                LOGGER.warn(exc);
            }
        });

        LOGGER.info("smart-socket server started on port {}", config.getPort());
    }

    protected void createSession(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        AioSession session = new AioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, true);
        session.initSession();
    }

    public final void shutdown() {
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            LOGGER.catching(e);
        }
        asynchronousChannelGroup.shutdown();
    }

    /**
     * 设置服务绑定的端口
     *
     * @param port
     * @return
     */
    public final AioQuickServer<T> bind(int port) {
        this.config.setPort(port);
        return this;
    }

    /**
     * 设置处理线程数量
     *
     * @param num
     * @return
     */
    public final AioQuickServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }

    public final AioQuickServer<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public final AioQuickServer<T> setFilters(Filter<T>... filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public final AioQuickServer<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     * @return
     */
    public final AioQuickServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size
     * @return
     */
    public final AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled
     * @return
     */
    public final AioQuickServer<T> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }
}
