/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * AIO服务端
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AioQuickServer.class);
    protected AsynchronousServerSocketChannel serverSocketChannel = null;
    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected IoServerConfig<T> config = new IoServerConfig<>();

    protected ReadCompletionHandler<T> aioReadCompletionHandler = new ReadCompletionHandler<>();
    protected WriteCompletionHandler<T> aioWriteCompletionHandler = new WriteCompletionHandler<>();

    /**
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            LOGGER.info(IoServerConfig.BANNER + "\r\n :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }
        start0();
    }

    protected final void start0() throws IOException {
        try {
            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
                byte index = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "smart-socket:AIO-" + (++index));
                }
            });
            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (Map.Entry<SocketOption<Object>, Object> entry : config.getSocketOptions().entrySet()) {
                    this.serverSocketChannel.setOption(entry.getKey(), entry.getValue());
                }
            }
            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), 1000);
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), 1000);
            }
            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                @Override
                public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                    serverSocketChannel.accept(attachment, this);
                    createSession(channel);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    LOGGER.error("smart-socket server accept fail", exc);
                }
            });
        } catch (IOException e) {
            shutdown();
            throw e;
        }
        LOGGER.info("smart-socket server started on port {}", config.getPort());
        LOGGER.info("smart-socket server config is {}", config);
    }

    protected void createSession(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        AioSession session = new AioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, true);
        session.initSession();
    }

    public final void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
            asynchronousChannelGroup = null;
        }
    }


    /**
     * 设置处理线程数量
     *
     * @param num
     */
    public final AioQuickServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }


    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     */
    public final AioQuickServer<T> setFilters(Filter<T>... filters) {
        this.config.setFilters(filters);
        return this;
    }


    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     */
    public final AioQuickServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size
     */
    public final AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled
     */
    public final AioQuickServer<T> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 是否启用DirectByteBuffer
     *
     * @param directBuffer
     */
    public final AioQuickServer<T> setDirectBuffer(boolean directBuffer) {
        config.setDirectBuffer(directBuffer);
        return this;
    }

    /**
     * @param host
     */
    public final AioQuickServer<T> setHost(String host) {
        config.setHost(host);
        return this;
    }

    /**
     *
     * @param socketOption
     * @param value
     * @param <V>
     * @return
     */
    public final <V> AioQuickServer<T> setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }
}
