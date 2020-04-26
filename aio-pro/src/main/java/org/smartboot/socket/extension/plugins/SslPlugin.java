/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.buffer.BufferFactory;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.ssl.SslAsynchronousSocketChannel;
import org.smartboot.socket.extension.ssl.SslService;

import java.io.InputStream;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public final class SslPlugin<T> extends AbstractPlugin<T> {
    private SslService sslService;
    private BufferPagePool bufferPagePool;
    private boolean init = false;

    public SslPlugin() {
        this.bufferPagePool = BufferFactory.DISABLED_BUFFER_FACTORY.create();
    }

    public SslPlugin(BufferPagePool bufferPagePool) {
        this.bufferPagePool = bufferPagePool;
    }

    public void initForServer(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword, ClientAuth clientAuth) {
        initCheck();
        sslService = new SslService(false, clientAuth);
        sslService.initKeyStore(keyStoreInputStream, keyStorePassword, keyPassword);
    }

    public void initForClient() {
        initForClient(null, null);
    }

    public void initForClient(InputStream trustInputStream, String trustPassword) {
        initCheck();
        sslService = new SslService(true, null);
        sslService.initTrust(trustInputStream, trustPassword);
    }

    private void initCheck() {
        if (init) {
            throw new RuntimeException("plugin is already init");
        }
        init = true;
    }

    @Override
    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new SslAsynchronousSocketChannel(channel, sslService, bufferPagePool.allocateBufferPage());
    }
}
