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
import java.util.concurrent.TimeUnit;

/**
 * AIO服务端。
 *
 * <h2>示例：</h2>
 * <p>
 * <pre>
 * public class IntegerServer {
 *     public static void main(String[] args) throws IOException {
 *         AioQuickServer<Integer> server = new AioQuickServer<Integer>(8888, new IntegerProtocol(), new IntegerServerProcessor());
 *         server.start();
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AioQuickServer.class);
    /**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected IoServerConfig<T> config = new IoServerConfig<>(true);
    /**
     * 读回调事件处理
     */
    protected ReadCompletionHandler<T> aioReadCompletionHandler = new ReadCompletionHandler<>();
    /**
     * 写回调事件处理
     */
    protected WriteCompletionHandler<T> aioWriteCompletionHandler = new WriteCompletionHandler<>();
    private Function<AsynchronousSocketChannel, AioSession<T>> aioSessionFunction;

    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * @param host             绑定服务端Host地址
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioQuickServer(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        this(port, protocol, messageProcessor);
        config.setHost(host);
    }

    /**
     * 启动Server端的AIO服务
     *
     * @throws IOException
     */
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            LOGGER.info(IoServerConfig.BANNER + "\r\n :: smart-socket ::\t(" + IoServerConfig.VERSION + ")");
        }
        start0(new Function<AsynchronousSocketChannel, AioSession<T>>() {
            @Override
            public AioSession<T> apply(AsynchronousSocketChannel channel) {
                return new AioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, true);
            }
        });
    }

    /**
     * 内部启动逻辑
     *
     * @throws IOException
     */
    protected final void start0(Function<AsynchronousSocketChannel, AioSession<T>> aioSessionFunction) throws IOException {
        try {
            this.aioSessionFunction = aioSessionFunction;
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

            serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
                @Override
                public void completed(final AsynchronousSocketChannel channel, AsynchronousServerSocketChannel serverSocketChannel) {
                    serverSocketChannel.accept(serverSocketChannel, this);
                    createSession(channel);
                }

                @Override
                public void failed(Throwable exc, AsynchronousServerSocketChannel serverSocketChannel) {
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

    /**
     * 为每个新建立的连接创建AIOSession对象
     *
     * @param channel
     */
    private void createSession(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        AioSession<T> session = null;
        try {
            session = aioSessionFunction.apply(channel);
            session.initSession();
        } catch (Exception e1) {
            LOGGER.debug(e1.getMessage(), e1);
            if (session == null) {
                try {
                    channel.shutdownInput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.shutdownOutput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.close();
                } catch (IOException e) {
                    LOGGER.debug("close channel exception", e);
                }
            } else {
                session.close();
            }

        }
    }

    /**
     * 停止服务端
     */
    public final void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        if (!asynchronousChannelGroup.isTerminated()) {
            try {
                asynchronousChannelGroup.shutdownNow();
            } catch (IOException e) {
                LOGGER.error("shutdown exception", e);
            }
        }
        try {
            asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("shutdown exception", e);
        }
    }


    /**
     * 设置处理线程数量
     *
     * @param num 线程数
     */
    public final AioQuickServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }


    /**
     * 设置输出队列缓冲区长度
     *
     * @param size 缓存队列长度
     */
    public final AioQuickServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     */
    public final AioQuickServer<T> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 设置Socket的TCP参数配置。
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * </p>
     *
     * @param socketOption 配置项
     * @param value        配置值
     * @return
     */
    public final <V> AioQuickServer<T> setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }
}
