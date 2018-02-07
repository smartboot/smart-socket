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
import org.smartboot.socket.http.handle.StaticResourceHandle;
import org.smartboot.socket.http.rfc2616.HttpHandle;
import org.smartboot.socket.http.rfc2616.HttpHandleGroup;
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
    private StaticResourceHandle defaultHandle = new StaticResourceHandle();

    public HttpMessageProcessor() {
        HttpHandleGroup.group().getCheckFilter()
                .next(new HttpHandle() {
                    @Override
                    public void doHandle(HttpRequest request, HttpResponse response) throws IOException {
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
        //文件上传body部分的数据流需要由业务处理，又不可影响IO主线程
        if (StringUtils.equalsIgnoreCase(entry.getMethod(), "POST") && StringUtils.equals(entry.getContentType(), "multipart/form-data")) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        process0(session, entry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                process0(session, entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stateEvent(AioSession<HttpRequest> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }

    private void process0(final AioSession<HttpRequest> session, HttpRequest request) throws IOException {
        HttpResponse httpResponse = new HttpResponse(request.getProtocol());
        HttpOutputStream outputStream = new HttpOutputStream(session, httpResponse);
        httpResponse.setOutputStream(outputStream);

        try {
            HttpHandleGroup.group().getCheckFilter().doHandle(request, httpResponse);
        } catch (Exception e) {
            httpResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpResponse.getOutputStream().write(e.fillInStackTrace().toString().getBytes());
        }
        httpResponse.getOutputStream().close();
        session.close(false);
    }

    public void route(String urlPattern, HttpHandle httpHandle) {
        handleMap.put(urlPattern, httpHandle);
    }
}
