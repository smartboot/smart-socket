package org.smartboot.socket.protocol.http;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StateMachineEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpServerMessageProcessor implements MessageProcessor<HttpEntity> {
    private static final Logger LOGGER = LogManager.getLogger(HttpServerMessageProcessor.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void process(final AioSession<HttpEntity> session, final HttpEntity entry) {
        //文件上传body部分的数据流需要由业务处理，又不可影响IO主线程
        if (StringUtils.equalsIgnoreCase(entry.getMethod(), "POST")) {
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

    private void process0(AioSession<HttpEntity> session, HttpEntity entry) {
        ByteBuffer buffer = ByteBuffer.wrap(("HTTP/1.1 200 OK\n" +
                "Server: seer/1.4.4\n" +
                "Content-Length: 2\n" +
                ("Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection")) ?
                        "Connection: keep-alive\n" : ""
                ) +
                "\n" +
                "OK").getBytes());
        try {
//            buffer.flip();
            session.write(buffer);
        } catch (IOException e) {
            LOGGER.catching(e);
        }
//        System.out.println(entry);
        if (!"Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection"))) {
            session.close(false);
        }
    }

    @Override
    public void stateEvent(AioSession<HttpEntity> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }
}
