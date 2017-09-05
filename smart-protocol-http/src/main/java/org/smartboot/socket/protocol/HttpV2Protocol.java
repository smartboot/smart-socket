package org.smartboot.socket.protocol;

import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.protocol.strategy.FormDataBoundaryStrategy;
import org.smartboot.socket.protocol.strategy.FormUrlencodedContentLengthStrategy;
import org.smartboot.socket.protocol.strategy.PostDecodeStrategy;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by seer on 2017/6/20.
 */
public class HttpV2Protocol implements Protocol<HttpV2Entity> {
    private static final String HTTP_ENTITY = "_http_entity_";
    private static final String CONTENT_LENGTH_KEY = "CONTENT_KEY";
    private static final String FORM_DATA_BOUNDARY_KEY = "form-data_boundary";
    private Map<String, PostDecodeStrategy> strategyMap = new HashMap<>();

    {
        strategyMap.put(CONTENT_LENGTH_KEY, new FormUrlencodedContentLengthStrategy());
        strategyMap.put(FORM_DATA_BOUNDARY_KEY, new FormDataBoundaryStrategy());
    }

    @Override
    public HttpV2Entity decode(ByteBuffer buffer, AioSession<HttpV2Entity> session) {
        HttpV2Entity entity = session.getAttribute(HTTP_ENTITY);
        if (entity == null) {
            entity = new HttpV2Entity(session);
            session.setAttribute(HTTP_ENTITY, entity);
        }
        boolean returnEntity = false;//是否返回HttpEntity
        boolean continueDecode = true;//是否继续读取数据
        while (buffer.hasRemaining() && continueDecode) {
            switch (entity.partFlag) {
                case HEAD: {
                    if (entity.dataStream.append(buffer.get())) {
                        entity.decodeHead();//消息头解码
                        if (StringUtils.equalsIgnoreCase("POST", entity.getMethod()) && entity.getContentLength() != 0) {
                            entity.partFlag = HttpPart.BODY;
                            selectDecodeStrategy(entity);//识别body解码处理器
                            if (!entity.postDecodeStrategy.waitForBodyFinish()) {
                                returnEntity = true;
                            }
                        } else {
                            entity.partFlag = HttpPart.END;
                            returnEntity = true;
                            continueDecode = false;
                        }
                    }
                    break;
                }
                case BODY: {
//                    System.out.println(entity.postDecodeStrategy);
                    if (entity.postDecodeStrategy.isDecodeEnd(buffer.get(), entity)) {
                        entity.partFlag = HttpPart.END;
                        if (entity.postDecodeStrategy.waitForBodyFinish()) {
                            returnEntity = true;
                        }
                    }

//                    //普通form表单,全量解析
//                    if (entity.getContentType() == null || StringUtils.startsWith(entity.getContentType(), "application/x-www-form-urlencoded")) {
//                        //识别body长度
//                        if (entity.dataStream.getContentLength() <= 0) {
//                            entity.dataStream.setContentLength(entity.getContentLength());
//                        }
//                        if (entity.dataStream.append(buffer.get())) {
//                            entity.decodeNormalBody();
//                            entity.partFlag = HttpPart.END;
//                            returnEntity = true;
//                        }
//                    }
//                    //二进制文件以流形式处理
//                    else if (StringUtils.startsWith(entity.getContentType(), "multipart/form-data")) {
//                        if (entity.getHeadMap().containsKey(HttpV2Entity.CONTENT_LENGTH)) {
//                            if (entity.getContentLength() > 0) {
//                                try {
//                                    entity.binaryBuffer.put(buffer.get());
//                                    entity.binReadLength++;
//                                    if (entity.binReadLength == entity.getContentLength()) {
//                                        entity.partFlag = HttpPart.END;
//                                        break;
//                                    }
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            } else {
//                                throw new RuntimeException("unkonw length");
//                            }
//                        } else if (entity.getHeadMap().containsKey(HttpV2Entity.TRANSFER_ENCODING)) {
//                            //解析chunkedBlockSize
//                            if (entity.chunkedBlockSize < 0) {
//                                entity.dataStream.setEndFLag("\r\n".getBytes());
//                                if (entity.dataStream.append(buffer.get())) {
//                                    int blockSize = NumberUtils.toInt(entity.dataStream.toString(), -1);
//                                    if (blockSize < 0) {
//                                        throw new RuntimeException("decode block size error:" + entity.dataStream.toString());
//                                    }
//                                    entity.chunkedBlockSize = blockSize;
//                                    entity.dataStream.reset();
//                                    //解码完成
//                                    if (entity.chunkedBlockSize == 0) {
//                                        entity.partFlag = HttpPart.END;
//                                        break;
//                                    }
//                                }
//                            } else {
//                                try {
//                                    entity.binaryBuffer.put(buffer.get());
//                                    entity.binReadLength++;
//                                    if (entity.binReadLength == entity.chunkedBlockSize) {
//                                        entity.binReadLength = 0;
//                                        entity.chunkedBlockSize = -1;
//                                    }
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    }
                    break;
                }
                default: {
                    session.removeAttribute(HTTP_ENTITY);
                }
            }
        }
        if (entity.partFlag == HttpPart.END) {
            session.removeAttribute(HTTP_ENTITY);
        }
        return returnEntity ? entity : null;
    }

    @Override
    public ByteBuffer encode(HttpV2Entity httpEntity, AioSession<HttpV2Entity> session) {
        return null;
    }

    private void selectDecodeStrategy(HttpV2Entity entity) {
        if (entity.getContentLength() > 0 && StringUtils.startsWith(entity.getContentType(), "application/x-www-form-urlencoded")) {
            entity.postDecodeStrategy = strategyMap.get(CONTENT_LENGTH_KEY);
        } else if (StringUtils.startsWith(entity.getContentType(), "multipart/form-data; boundary") && entity.getContentLength() > 0) {
            entity.postDecodeStrategy = strategyMap.get(FORM_DATA_BOUNDARY_KEY);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
