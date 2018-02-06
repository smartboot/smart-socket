/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpsBootstrap.java
 * Date: 2018-02-04
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.transport.AioSSLQuickServer;

import java.io.IOException;

public class HttpsBootstrap {

    public static void main(String[] args) throws ClassNotFoundException {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        AioSSLQuickServer<HttpRequest> server = new AioSSLQuickServer<HttpRequest>(8889, new HttpProtocol(), new HttpMessageProcessor());
        server
                .setClientAuth(ClientAuth.OPTIONAL)
                .setKeyStore("server.jks", "storepass")
                .setTrust("trustedCerts.jks", "storepass")
                .setKeyPassword("keypass")
//                .setThreadNum(8)
//                .setWriteQueueSize(1)
        ;
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
