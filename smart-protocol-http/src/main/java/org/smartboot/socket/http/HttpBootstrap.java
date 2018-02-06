/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.Filter;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.net.UnknownHostException;

public class HttpBootstrap {

    public static void main(String[] args) throws UnknownHostException {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        AioQuickServer<HttpRequest> server = new AioQuickServer<HttpRequest>(8888, new HttpProtocol(), new HttpMessageProcessor());
        server.setFilters(new Filter[]{new QuickMonitorTimer<HttpRequest>()});
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
