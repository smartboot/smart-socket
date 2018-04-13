/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpServerMessageProcessor.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.http11.DefaultHttpResponse;
import org.smartboot.socket.http.http11.Http11HandleGroup;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpMessageProcessor implements MessageProcessor<HttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMessageProcessor.class);
    private Http11HandleGroup http11HandleGroup = null;

    public HttpMessageProcessor(String baseDir) {
        http11HandleGroup = new Http11HandleGroup(baseDir);
    }


    @Override
    public void process(final AioSession<HttpRequest> session, final HttpRequest entry) {
        if (entry instanceof Http11Request) {
            final Http11Request request = (Http11Request) entry;
            try {
                processHttp11(session, request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stateEvent(AioSession<HttpRequest> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    private static String b="HTTP/1.1 200 OK\r\n" +
            "Server:smart-socket\r\n" +
            "Connection:keep-alive\r\n" +
            "Host:localhost\r\n" +
            "Content-Length:31\r\n" +
            "Date:Wed, 11 Apr 2018 12:35:01 GMT\r\n\r\n"+
            "Hello smart-socket http server!";
    private void processHttp11(final AioSession<HttpRequest> session, Http11Request request) throws IOException {
//        HttpResponse httpResponse = new DefaultHttpResponse(session, request, http11HandleGroup);
//        try {
//            http11HandleGroup.getPreHandle().doHandle(request, httpResponse);
//        } catch (Exception e) {
//            LOGGER.debug("", e);
//            httpResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//            httpResponse.getOutputStream().write(e.fillInStackTrace().toString().getBytes());
//        }
//
//        httpResponse.getOutputStream().close();
//
//        if (!StringUtils.equalsIgnoreCase(HttpHeaderConstant.Values.KEEPALIVE, request.getHeader(HttpHeaderConstant.Names.CONNECTION)) || httpResponse.getHttpStatus() != HttpStatus.OK) {
//            session.close(false);
//        }
    session.write(ByteBuffer.wrap(b.getBytes()));
    }

    public void route(String urlPattern, HttpHandle httpHandle) {
        http11HandleGroup.getRouteHandle().route(urlPattern, httpHandle);
    }
}
