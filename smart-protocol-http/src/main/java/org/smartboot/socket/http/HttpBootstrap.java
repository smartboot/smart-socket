/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.Filter;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSSLQuickServer;

import java.io.IOException;
import java.net.UnknownHostException;

public class HttpBootstrap {

    public static void main(String[] args) throws UnknownHostException {
        HttpMessageProcessor processor=new HttpMessageProcessor();

        http();
    }

    static void http() {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        AioQuickServer<HttpRequest> server = new AioQuickServer<HttpRequest>(8888, new HttpProtocol(), new HttpMessageProcessor());
        server.setFilters(new Filter[]{new QuickMonitorTimer<HttpRequest>()});
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void https() {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        AioSSLQuickServer<HttpRequest> server = new AioSSLQuickServer<HttpRequest>(8889, new HttpProtocol(), new HttpMessageProcessor());
        server
                .setClientAuth(ClientAuth.OPTIONAL)
                .setKeyStore("server.jks", "storepass")
                .setTrust("trustedCerts.jks", "storepass")
                .setKeyPassword("keypass")
        ;
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
