/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
 * Date: 2017-11-25 10:29:55
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.ssl.HandshakeCallback;
import org.smartboot.socket.extension.ssl.HandshakeModel;
import org.smartboot.socket.extension.ssl.SSLConfig;
import org.smartboot.socket.extension.ssl.SSLService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * AIO服务端
 * Created by 三刀 on 2017/6/28.
 */
public class AioQuickServer<T> {
    private static final Logger LOGGER = LogManager.getLogger(AioQuickServer.class);
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;
    private IoServerConfig<T> config = new IoServerConfig<>();
    private SSLConfig sslConfig = new SSLConfig();
    private ReadCompletionHandler<T> aioReadCompletionHandler = new ReadCompletionHandler<>();
    private WriteCompletionHandler<T> aioWriteCompletionHandler = new WriteCompletionHandler<>();
    private SSLService sslService;

    public void start() throws IOException {
        //启动SSL服务
        if (config.isSsl()) {
            sslService = new SSLService(sslConfig);
        }
        asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
            byte index = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "AIO-Thread-" + (++index));
            }
        });
        this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress(config.getPort()), 1000);
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                serverSocketChannel.accept(attachment, this);
                //连接成功则构造AIOSession对象
                if (config.isSsl()) {
                    final SSLAioSession sslAioSession = new SSLAioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, true);
                    final HandshakeModel handshakeModel = sslService.createSSLEngine();
                    handshakeModel.setSocketChannel(channel);
                    handshakeModel.setHandshakeCallback(new HandshakeCallback() {
                        @Override
                        public void callback() {
                            sslAioSession.netReadBuffer=handshakeModel.getNetWriteBuffer();
                            sslAioSession.netWriteBuffer=handshakeModel.getNetWriteBuffer();
                            sslAioSession.netReadBuffer.clear();
                            sslAioSession.netWriteBuffer.clear();
                            sslAioSession.readFromChannel(false);
                        }
                    });
                    sslService.doHandshake(handshakeModel);
                } else {
                    AioSession session = new AioSession<T>(channel, config, aioReadCompletionHandler, aioWriteCompletionHandler, true);
                    session.readFromChannel(false);
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                LOGGER.warn(exc);
            }
        });
        LOGGER.info("smart-socket server started on port {}", config.getPort());
    }

    public void shutdown() {
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
    public AioQuickServer<T> bind(int port) {
        this.config.setPort(port);
        return this;
    }

    /**
     * 设置处理线程数量
     *
     * @param num
     * @return
     */
    public AioQuickServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }

    public AioQuickServer<T> setProtocol(Protocol<T> protocol) {
        this.config.setProtocol(protocol);
        return this;
    }

    /**
     * 设置消息过滤器,执行顺序以数组中的顺序为准
     *
     * @param filters
     * @return
     */
    public AioQuickServer<T> setFilters(Filter<T>... filters) {
        this.config.setFilters(filters);
        return this;
    }

    /**
     * 设置消息处理器
     *
     * @param processor
     * @return
     */
    public AioQuickServer<T> setProcessor(MessageProcessor<T> processor) {
        this.config.setProcessor(processor);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度
     *
     * @param size
     * @return
     */
    public AioQuickServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    public AioQuickServer<T> setSsl(boolean flag) {
        this.config.setSsl(flag);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size
     * @return
     */
    public AioQuickServer<T> setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }


    public AioQuickServer<T> setKeyStore(String keyStoreFile, String keystorePassword) {
        sslConfig.setKeyFile(keyStoreFile);
        sslConfig.setKeystorePassword(keystorePassword);
        return this;
    }

    public AioQuickServer<T> setKeyPassword(String keyPassword) {
        sslConfig.setKeyPassword(keyPassword);
        return this;
    }

    public AioQuickServer<T> setTrust(String trustFile, String trustPassword) {
        sslConfig.setTrustFile(trustFile);
        sslConfig.setTrustPassword(trustPassword);
        return this;
    }
}
