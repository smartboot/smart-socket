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
import org.smartboot.socket.http.rfc2616.HttpFilter;
import org.smartboot.socket.http.rfc2616.HttpFilterGroup;
import org.smartboot.socket.http.route.Route;
import org.smartboot.socket.http.route.StaticResourceRoute;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private List<Route> routeList = new ArrayList<>();

    {
        routeList.add(new StaticResourceRoute());
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

        HttpFilterGroup.group().getCheckFilter().next(new HttpFilter() {
            @Override
            public void doFilter(final HttpRequest request, HttpResponse response) throws IOException {
                Route route = null;
                for (Route r : routeList) {
                    if (request.getOriginalUri().matches(r.urlPattern())) {
                        route = r;
                        break;
                    }
                }
                if (route == null) {
                    response.setHttpStatus(HttpStatus.NOT_FOUND);
                    return;
                }
                route.process(request, response);

            }
        }).doFilter(request, httpResponse);
        httpResponse.getOutputStream().close();
        session.close(false);
    }

}
