package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import net.vinote.smart.socket.transport.IoSession;

import java.nio.ByteBuffer;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public final class HttpServerMessageProcessor extends AbstractServerDataGroupProcessor<HttpEntity> {

    @Override
    public void process(IoSession<HttpEntity> session, HttpEntity entry) throws Exception {
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
        if (!"Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection"))) {
            session.close(false);
        }
    }

    @Override
    public void initSession(IoSession<HttpEntity> session) {

    }
}
