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
import org.smartboot.socket.extension.ssl.SSLConfig;
import org.smartboot.socket.extension.ssl.SSLService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * AIO实现的客户端服务
 * Created by 三刀 on 2017/6/28.
 */
public final class AioSSLQuickClient<T> extends AioQuickClient<T> {
    private SSLService sslService;

    private SSLConfig sslConfig = new SSLConfig();

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
        sslService = new SSLService(sslConfig);
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        session = new SSLAioSession<T>(socketChannel, config, new ReadCompletionHandler<T>(), new WriteCompletionHandler<T>(), sslService);
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
