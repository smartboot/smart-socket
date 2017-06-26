package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.transport.TransportSession;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by zhengjunwei on 2017/6/20.
 */
public class HttpProtocol implements Protocol<HttpEntity> {
    private static final String CRFL = "\r\n";
    private static final String HTTP_ENTITY = "_http_entity_";

    @Override
    public HttpEntity decode(ByteBuffer buffer, TransportSession<HttpEntity> session) {
        HttpEntity entity = session.getAttribute(HTTP_ENTITY);
        if (entity == null) {
            entity = new HttpEntity(session);
            session.setAttribute(HTTP_ENTITY, entity);
        }
        if (!entity.endOfHeader) {
            if (buffer == null && buffer.remaining() >= 2) {
                return null;
            }
            boolean existsLine = false;//是否存在Header Line
            do {
                int maxLength = buffer.remaining();
                for (int i = 1; i < maxLength; i++) {
                    if (buffer.get(buffer.position() + i - 1) == '\r' && buffer.get(buffer.position() + i) == '\n') {
                        byte[] data = new byte[i + 1];
                        buffer.get(data);
                        if (i == 1) {
                            session.pauseReadAttention();
                            entity.endOfHeader = true;
                            return entity;
                        }
                        entity.getHeader().lineList.add(new HeadLine(new String(data, 0, data.length - 2)));
                        existsLine = true;
                        break;
                    }
                }
            } while (existsLine);
        } else {
            entity.appendBodyStream(buffer);
        }


        return null;
    }

    private int indexOf(ByteBuffer data) {
//        data.
        return -1;
    }
}
