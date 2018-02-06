/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.socket.http;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HttpBootstrap {

    public static void main(String[] args) throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost());
        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        AioQuickServer<HttpRequest> server = new AioQuickServer<HttpRequest>(8888, new HttpProtocol(), new HttpServerMessageProcessor());
//        server.setFilters(new Filter[]{new QuickMonitorTimer<HttpRequest>()});
//        try {
//            server.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
