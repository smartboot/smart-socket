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
import org.smartboot.socket.extension.ssl.SSLConfig;
import org.smartboot.socket.extension.ssl.SSLService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * AIO实现的客户端服务
 * Created by 三刀 on 2017/6/28.
 */
public class AioQuickClient<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickClient.class);
    private AsynchronousSocketChannel socketChannel = null;
    /**
     * IO事件处理线程组
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private SSLService sslService;
    /**
     * 客户端服务配置
     */
    private IoServerConfig<T> config = new IoServerConfig<T>();

    private SSLConfig sslConfig = new SSLConfig();

    /**
     * @param asynchronousChannelGroup
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        //启动SSL服务
        if (config.isSsl()) {
            sslConfig.setClientMode(true);
            sslService = new SSLService(sslConfig);
        }
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        AioSession session;
        if (config.isSsl()) {
            session = new SSLAioSession<T>(socketChannel, config, new ReadCompletionHandler(), new WriteCompletionHandler(), sslService);
        } else {
            session = new AioSession<T>(socketChannel, config, new ReadCompletionHandler(), new WriteCompletionHandler(), false);
        }
        session.initSession();
    }

    /**
     * 启动客户端Socket服务
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void start() throws IOException, ExecutionException, InterruptedException {
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
    public void shutdown() {
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
    public AioQuickClient<T> connect(String host, int port) {
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
    public AioQuickClient<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public AioQuickClient<T> setFilters(Filter<T>[] filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public AioQuickClient<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    public AioQuickClient<T> setSsl(boolean flag) {
        this.config.setSsl(flag);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size
     * @return
     */
    public AioQuickClient<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    public AioQuickClient<T> setKeyStore(String keyStoreFile, String keystorePassword) {
        sslConfig.setKeyFile(keyStoreFile);
        sslConfig.setKeystorePassword(keystorePassword);
        return this;
    }


    public AioQuickClient<T> setKeyPassword(String keyPassword) {
        sslConfig.setKeyPassword(keyPassword);
        return this;
    }

    public AioQuickClient<T> setTrust(String trustFile, String trustPassword) {
        sslConfig.setTrustFile(trustFile);
        sslConfig.setTrustPassword(trustPassword);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     * @return
     */
    public AioQuickClient<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }
}
