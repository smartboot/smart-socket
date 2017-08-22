package org.smartboot.socket.protocol;

import org.smartboot.socket.service.process.ProtocolDataProcessor;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public final class HttpServerMessageProcessor implements ProtocolDataProcessor<HttpEntity> {

    @Override
    public void process(AioSession<HttpEntity> session, HttpEntity entry) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(("HTTP/1.1 200 OK\n" +
                "Server: seer/1.4.4\n" +
                "Content-Length: 2\n" +
                ("Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection")) ?
                        "Connection: keep-alive\n" : ""
                ) +
                "\n" +
                "OK").getBytes());
        session.write(buffer);
//        System.out.println(entry);
        if (!"Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection"))) {
            session.close(false);
        }
    }

    @Override
    public void initSession(AioSession<HttpEntity> session) {

    }
}
