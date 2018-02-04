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
import java.nio.ByteBuffer;
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
            process0(session, entry);
        }
    }

    @Override
    public void stateEvent(AioSession<HttpRequest> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }

    private void process0(AioSession<HttpRequest> session, HttpRequest entry) {
//        System.out.println(entry);
//        InputStream in=entry.getInputStream();
//        try {
//            FileOutputStream fos=new FileOutputStream("/Users/zhengjunwei/Downloads/1.png");
//
//        byte[] data=new byte[1023];
//        int size=0;
//        try {
//            while((size=in.read(data))!=-1){
//                fos.write(data,0,size);
////             sb.append(new String(data,0,size));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//            try {
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        HttpResponse httpResponse = new HttpResponse(entry.getProtocol());
        HttpOutputStream outputStream = new HttpOutputStream(session, httpResponse);
        httpResponse.setOutputStream(outputStream);


        ByteBuffer buffer = ByteBuffer.wrap(("HTTP/1.1 200 OK\n" +
                "Server: seer/1.4.4\n" +
                "Content-Length: 24\n" +
                ("Keep-Alive".equalsIgnoreCase(entry.getHeader("Connection")) ?
                        "Connection: keep-alive\n" : ""
                ) +
                "\n" +
                "smart-socket http server").getBytes());
        try {
//            buffer.flip();
            session.write(buffer);
        } catch (IOException e) {
            LOGGER.catching(e);
        }
//        System.out.println(entry);
        if (!"Keep-Alive".equalsIgnoreCase(entry.getHeader("Connection"))) {
            session.close(false);
        }
    }


}
