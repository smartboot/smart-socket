/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

public class HttpBootstrap {

    public static void main(String[] args) throws ClassNotFoundException {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor();
        AioQuickServer<HttpV2Entity> server = new AioQuickServer<HttpV2Entity>()
                .setThreadNum(8)
                .setProtocol(new HttpV2Protocol())
                .setWriteQueueSize(1)
//                .setFilters(new QuickMonitorTimer<HttpV2Entity>())
                .setProcessor(processor)
//                .setSsl(true)
                .setClientAuth(ClientAuth.OPTIONAL)
                .setKeyStore("server.jks", "storepass")
                .setTrust("trustedCerts.jks", "storepass")
                .setKeyPassword("keypass");
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
