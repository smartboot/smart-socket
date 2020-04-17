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
import org.smartboot.socket.extension.ssl.SslConfig;
import org.smartboot.socket.extension.ssl.SslService;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public class SslServerPlugin<T> extends SslPlugin<T> {

    public SslServerPlugin(ClientAuth clientAuth, SslConfig sslConfig) {
        sslService = new SslService(clientAuth, sslConfig);
    }

}
