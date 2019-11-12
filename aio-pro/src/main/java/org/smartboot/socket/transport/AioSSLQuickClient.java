/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSSLQuickClient.java
 * Date: 2018-02-04
 * Author: sandao
 */


package org.smartboot.socket.transport;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.ssl.SslConfig;
import org.smartboot.socket.extension.ssl.SslService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * AIO实现的客户端服务
 *
 * @author 三刀
 */
public final class AioSSLQuickClient<T> extends AioQuickClient<T> {
    private SslService sslService;

    private SslConfig sslConfig = new SslConfig();

    /**
     * @param host             远程服务器地址
     * @param port             远程服务器端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioSSLQuickClient(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        super(host, port, protocol, messageProcessor);
    }

    /**
     * @param asynchronousChannelGroup
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public AioSession<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        //启动SSL服务
        sslConfig.setClientMode(true);
        sslService = new SslService(sslConfig);
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        session = new SslAioSession<T>(socketChannel, config, new ReadCompletionHandler<T>(), new WriteCompletionHandler<T>(), sslService, bufferPool.allocateBufferPage());
        session.initSession();
        return session;
    }


    public AioSSLQuickClient<T> setKeyStore(String keyStoreFile, String keystorePassword) {
        sslConfig.setKeyFile(keyStoreFile);
        sslConfig.setKeystorePassword(keystorePassword);
        return this;
    }


    public AioSSLQuickClient<T> setKeyPassword(String keyPassword) {
        sslConfig.setKeyPassword(keyPassword);
        return this;
    }

    public AioSSLQuickClient<T> setTrust(String trustFile, String trustPassword) {
        sslConfig.setTrustFile(trustFile);
        sslConfig.setTrustPassword(trustPassword);
        return this;
    }


}
