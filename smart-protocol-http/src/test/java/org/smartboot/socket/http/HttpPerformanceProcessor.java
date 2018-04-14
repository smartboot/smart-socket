/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpServerMessageProcessor.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpPerformanceProcessor implements MessageProcessor<HttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPerformanceProcessor.class);
    private static String b = "HTTP/1.1 200 OK\r\n" +
            "Server:smart-socket\r\n" +
            "Connection:keep-alive\r\n" +
            "Host:localhost\r\n" +
            "Content-Length:31\r\n" +
            "Date:Wed, 11 Apr 2018 12:35:01 GMT\r\n\r\n" +
            "Hello smart-socket http server!";

    @Override
    public void process(final AioSession<HttpRequest> session, final HttpRequest entry) {
        try {
            session.write(ByteBuffer.wrap(b.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<HttpRequest> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }


}
