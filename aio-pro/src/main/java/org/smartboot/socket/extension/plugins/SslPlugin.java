/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.ssl.SslAsynchronousSocketChannel;
import org.smartboot.socket.extension.ssl.SslService;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.extension.ssl.factory.SSLContextFactory;
import org.smartboot.socket.extension.ssl.factory.ServerSSLContextFactory;

import javax.net.ssl.SSLEngine;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

/**
 * SSL/TLS通信插件
 * <p>
 * 证书生成工具：https://github.com/FiloSottile/mkcert
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public final class SslPlugin<T> extends AbstractPlugin<T> {
    private final SslService sslService;
    private final BufferPagePool pool;

    public SslPlugin(SSLContextFactory factory, Consumer<SSLEngine> consumer) throws Exception {
        this(factory, consumer, BufferPagePool.DEFAULT_BUFFER_PAGE_POOL);
    }

    public SslPlugin(SSLContextFactory factory) throws Exception {
        this(factory, null);
    }

    public SslPlugin(SSLContextFactory factory, Consumer<SSLEngine> consumer, BufferPagePool pool) throws Exception {
        this.pool = pool;
        sslService = new SslService(factory.create(), sslEngine -> {
            factory.initSSLEngine(sslEngine);
            if (consumer != null) {
                consumer.accept(sslEngine);
            }
        });
    }

    public SslPlugin(ClientSSLContextFactory factory) throws Exception {
        this(factory, BufferPagePool.DEFAULT_BUFFER_PAGE_POOL);
    }

    public SslPlugin(ClientSSLContextFactory factory, BufferPagePool pool) throws Exception {
        this(factory, null, pool);
    }

    public SslPlugin(ServerSSLContextFactory factory, ClientAuth clientAuth) throws Exception {
        this(factory, clientAuth, BufferPagePool.DEFAULT_BUFFER_PAGE_POOL);
    }

    public SslPlugin(ServerSSLContextFactory factory, ClientAuth clientAuth, BufferPagePool pool) throws Exception {
        this(factory, sslEngine -> {
            switch (clientAuth) {
                case OPTIONAL:
                    sslEngine.setWantClientAuth(true);
                    break;
                case REQUIRE:
                    sslEngine.setNeedClientAuth(true);
                    break;
                case NONE:
                    break;
                default:
                    throw new Error("Unknown auth " + clientAuth);
            }
        }, pool);
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new SslAsynchronousSocketChannel(channel, sslService, pool);
    }

    public void debug(boolean debug) {
        sslService.debug(debug);
    }
}
