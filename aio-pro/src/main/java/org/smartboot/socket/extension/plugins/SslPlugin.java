/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.extension.ssl.SslAsynchronousSocketChannel;
import org.smartboot.socket.extension.ssl.SslService;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
abstract class SslPlugin<T> extends AbstractPlugin<T> {
    protected SslService sslService;

    SslPlugin() {
    }

    @Override
    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new SslAsynchronousSocketChannel(channel, sslService, 4096);
    }
}
