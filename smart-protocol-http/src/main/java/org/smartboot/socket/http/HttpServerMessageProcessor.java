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
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpServerMessageProcessor implements MessageProcessor<HttpRequest> {
    private static final Logger LOGGER = LogManager.getLogger(HttpServerMessageProcessor.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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

    private void process0(AioSession<HttpRequest> session, HttpRequest entry) throws IOException {
        HttpResponse httpResponse = new HttpResponse(entry.getProtocol());
        HttpOutputStream outputStream = new HttpOutputStream(session, httpResponse);
        InputStream in = entry.getInputStream();
        byte[] bytes = new byte[1024];
        int readSize = 0;
        while ((readSize = in.read(bytes)) != -1) {
            System.out.println(new String(bytes, 0, readSize));
        }
        httpResponse.setOutputStream(outputStream);
        httpResponse.setHttpStatus(HttpStatus.OK);
        httpResponse.setHeader("Content-Length", "24");
        try {
            outputStream.write("smart-socket http server".getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }


}
