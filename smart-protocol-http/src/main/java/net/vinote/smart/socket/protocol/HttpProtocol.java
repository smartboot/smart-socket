package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.lang.StringUtils;
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
        } else {
            int contentLength = entity.getContentLength();

            //根据Content-Length解码
            if (contentLength > 0) {
                System.out.println("contentLength:"+contentLength);
                int num = buffer.remaining();
                int needLength = contentLength - entity.getReadBodyLength().get();
                if (num <= needLength) {
                    entity.appendBodyStream(buffer);
                    entity.getReadBodyLength().addAndGet(num);
                } else {
                    byte[] data = new byte[needLength];
                    buffer.get(data);
                    entity.appendBodyStream(data);
                    entity.getReadBodyLength().addAndGet(needLength);
                }
                if (contentLength == entity.getReadBodyLength().get()) {
                    session.reachEndOfStream();
                    session.removeAttribute(HTTP_ENTITY);
                    System.out.println("解码完成");
                }

            }
            //根据Transfer_Encoding解码
            else if ("chunked".equalsIgnoreCase(entity.getHeadMap().get(HttpEntity.TRANSFER_ENCODING))) {
                System.out.println("chunked 解码");
                //解析chunked size
                if (entity.getChunked() == -1) {
                    int maxLength = buffer.remaining();
                    for (int i = 1; i < maxLength; i++) {
                        if (buffer.get(buffer.position() + i - 1) == '\r' && buffer.get(buffer.position() + i) == '\n') {
                            byte[] chunkedBytes = new byte[i - 1];
                            buffer.get(chunkedBytes);
                            int chunked = Integer.valueOf(new String(chunkedBytes).trim());
                            System.out.println("chunked:" + chunked);
                            entity.setChunked(chunked);
                            buffer.get();//\r
                            buffer.get();//\n
                            break;
                        }
                    }

                }
                if (entity.getChunked() == -1) {//未解析到数据块大小
                    return null;
                } else if (entity.getChunked() == 0) {
                    session.reachEndOfStream();
                    session.removeAttribute(HTTP_ENTITY);
                    System.out.println("解码完成");
                    return null;
                }

                int num = buffer.remaining();
                int needLength = entity.getChunked() - entity.getReadBodyLength().get();
                if (num <= needLength) {
                    entity.appendBodyStream(buffer);
                    entity.getReadBodyLength().addAndGet(num);

                } else {
                    byte[] data = new byte[needLength];
                    buffer.get(data);
                    entity.appendBodyStream(data);
                    entity.getReadBodyLength().addAndGet(needLength);
                }
                //本轮数据块已读完
                if (entity.getChunked() == entity.getReadBodyLength().get()) {
                    entity.getReadBodyLength().set(0);//清空已读数据
                    entity.setChunked(-1);
                }
            }else{
                System.out.println("无知body");
                int maxLength = buffer.remaining();
                for (int i = 1; i < maxLength-1; i++) {
                    if (buffer.get(buffer.position() + i - 1) == '\r' && buffer.get(buffer.position() + i) == '\n') {
                        byte[] data = new byte[i + 1];
                        buffer.get(data);
                        entity.appendBodyStream(data);
                        break;
                    }
                }
                session.reachEndOfStream();
                session.removeAttribute(HTTP_ENTITY);
            }

        }


        return null;
    }


    private int indexOf(ByteBuffer data) {
//        data.
        return -1;
    }


}
