package org.smartboot.socket.protocol;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StringUtils;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by seer on 2017/6/20.
 */
public class HttpV2Protocol implements Protocol<HttpV2Entity> {
    private static final String CRFL = "\r\n";
    private static final String HTTP_ENTITY = "_http_entity_";

    @Override
    public HttpV2Entity decode(ByteBuffer buffer, AioSession<HttpV2Entity> session) {
        HttpV2Entity entity = session.getAttribute(HTTP_ENTITY);
        if (entity == null) {
            entity = new HttpV2Entity(session);
            session.setAttribute(HTTP_ENTITY, entity);
        }
        boolean suc = false;
        while (buffer.hasRemaining()) {
            switch (entity.partFlag) {
                case 0: {
                    if (entity.headStream.append(buffer.get())) {
                        entity.decodeHead();//消息头解码
                        //POST请求可能存在消息体
                        entity.partFlag = StringUtils.equalsIgnoreCase("POST", entity.getMethod()) ? 1 : 2;
                        //非Post请求,则解码完成
                        if(!StringUtils.equalsIgnoreCase("POST", entity.getMethod())) {
                            suc = true;
                        }
                    }
                    break;
                }
                case 1: {
                    //普通form表单,全量解析
                    if(StringUtils.startsWith(entity.getContentType(),"application/x-www-form-urlencoded")){

                        if(entity.bodyStream.append(buffer.get())){
                            entity.decodeBody();
                            entity.partFlag=2;
                            suc=true;
                        }
                    }
                    //二进制文件以流形式处理
                    if(StringUtils.startsWith(entity.getContentType(),"multipart/form-data")){
                        if(entity.bodyDecodeType==HttpV2Entity.BODY_DECODE_LENGTH) {
                            if (entity.getContentLength() > 0 && entity.binReadLength < entity.getContentLength()) {
                                try {
                                    entity.binaryBuffer.put(buffer.get());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }else if(entity.bodyDecodeType==HttpV2Entity.BODY_DECODE_CHUNKED){

                        }
                        try {
                            entity.binaryBuffer.put(buffer.get());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                default: {
                    session.removeAttribute(HTTP_ENTITY);
                }
            }
        }
        if (entity.partFlag == 2) {
            session.removeAttribute(HTTP_ENTITY);
        }
        return suc ? entity : null;
    }

    @Override
    public ByteBuffer encode(HttpV2Entity httpEntity, AioSession<HttpV2Entity> session) {
        return null;
    }


}
