/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Test1.java
 * Date: 2018-01-20 13:31:09
 * Author: sandao
 */

import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.http.HttpServerMessageProcessor;
import org.smartboot.socket.http.HttpV2Entity;
import org.smartboot.socket.http.HttpV2Protocol;
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
                .setFilters(new QuickMonitorTimer<HttpV2Entity>())
                .setProcessor(processor)
                .setSsl(true)
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
