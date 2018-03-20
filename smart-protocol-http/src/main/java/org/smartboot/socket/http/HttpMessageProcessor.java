/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpServerMessageProcessor.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.handle.StaticResourceHandle;
import org.smartboot.socket.http.http11.DefaultHttpResponse;
import org.smartboot.socket.http.http11.Http11HandleGroup;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpMessageProcessor implements MessageProcessor<HttpRequest> {
    private static final Logger LOGGER = LogManager.getLogger(HttpMessageProcessor.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Map<String, HttpHandle> handleMap = new HashMap<>();
    private StaticResourceHandle defaultHandle;

    public HttpMessageProcessor(String baseDir) {
        defaultHandle = new StaticResourceHandle(baseDir);
        Http11HandleGroup.group().getHttpHandle()
                .next(new HttpHandle() {
                    @Override
                    public void doHandle(Http11Request request, HttpResponse response) throws IOException {
                        HttpHandle httpHandle = null;
                        for (Map.Entry<String, HttpHandle> entity : handleMap.entrySet()) {

                            if (request.getRequestURI().matches(entity.getKey())) {
                                httpHandle = entity.getValue();
                                break;
                            }
                        }
                        if (httpHandle == null) {
                            httpHandle = defaultHandle;
                        }
                        httpHandle.doHandle(request, response);
                    }
                });
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

    }

    private void processHttp11(final AioSession<HttpRequest> session, Http11Request request) throws IOException {
        HttpResponse httpResponse = new DefaultHttpResponse(session, request);
        try {
            Http11HandleGroup.group().getHttpHandle().doHandle(request, httpResponse);
        } catch (Exception e) {
            LOGGER.catching(e);
            httpResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpResponse.getOutputStream().write(e.fillInStackTrace().toString().getBytes());
        }

        httpResponse.getOutputStream().close();
//        if (!StringUtils.equalsIgnoreCase(HttpHeaderConstant.Values.KEEPALIVE, request.getHeader(HttpHeaderConstant.Names.CONNECTION))) {
//            session.close(false);
//        }

//        session.close(false);
    }

    public void route(String urlPattern, HttpHandle httpHandle) {
        handleMap.put(urlPattern, httpHandle);
    }
}
