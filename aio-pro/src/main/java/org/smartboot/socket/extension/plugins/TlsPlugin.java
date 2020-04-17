/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.extension.tls.SslService;
import org.smartboot.socket.extension.tls.TlsAsynchronousSocketChannel;
import org.smartboot.socket.extension.tls.TlsConfig;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public class TlsPlugin<T> extends AbstractPlugin<T> {
    private SslService sslService;

    public TlsPlugin(TlsConfig sslConfig) {
        sslService = new SslService(sslConfig);
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new TlsAsynchronousSocketChannel(channel, sslService, 4096);
    }
}
