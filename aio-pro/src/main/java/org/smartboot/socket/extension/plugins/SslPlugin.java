/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

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

    public SslPlugin() {
        this(null, null);
    }

    public SslPlugin(InputStream trustInputStream, String trustPassword) {
        sslService = new SslService(true, null);
        sslService.initTrust(trustInputStream, trustPassword);
    }

    public SslPlugin(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword, ClientAuth clientAuth) {
        sslService = new SslService(false, clientAuth);
        sslService.initKeyStore(keyStoreInputStream, keyStorePassword, keyPassword);
    }

    @Override
    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new SslAsynchronousSocketChannel(channel, sslService, 4096);
    }
}
