package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.io.Channel;
import net.vinote.smart.socket.lang.StringUtils;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by zhengjunwei on 2017/6/20.
 */
public class HttpProtocol implements Protocol<HttpEntity> {
    private static final String CRFL = "\r\n";
    private static final String HTTP_ENTITY = "_http_entity_";

    @Override
    public HttpEntity decode(ByteBuffer buffer, Channel<HttpEntity> session) {
        HttpEntity entity = session.getAttribute(HTTP_ENTITY);
        if (entity == null) {
            entity = new HttpEntity(session);
            session.setAttribute(HTTP_ENTITY, entity);
        }
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
//                        session.pauseReadAttention();
                        session.removeAttribute(HTTP_ENTITY);
                        return entity;
                    }
                    String line = new String(data, 0, data.length - 2);
                    if (entity.getRequestLine() == null) {
                        String[] array = line.split(" ");
                        entity.setRequestLine(new RequestLine(array[0], array[1], array[2]));
                    } else {
                        entity.setHeader(StringUtils.substringBefore(line, ":").trim(), StringUtils.substringAfter(line, ":").trim());
                    }
                    existsLine = true;
                    break;
                }
            }
        } while (existsLine);
        return null;
    }

    @Override
    public ByteBuffer encode(HttpEntity httpEntity, Channel<HttpEntity> session) {
        return null;
    }


    private int indexOf(ByteBuffer data) {
//        data.
        return -1;
    }


}
