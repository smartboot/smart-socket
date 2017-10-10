package org.smartboot.socket.protocol;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.strategy.FormWithContentLengthStrategy;
import org.smartboot.socket.protocol.strategy.PostDecodeStrategy;
import org.smartboot.socket.protocol.strategy.StreamWithContentLengthStrategy;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
public class HttpV2Protocol implements Protocol<HttpV2Entity> {
    private static final Logger LOGGER = LogManager.getLogger(HttpV2Protocol.class);
    private static final String HTTP_ENTITY = "_http_entity_";
    private static final String STREAM_BODY = "STREAM_BODY";
    private static final String BLOCK_BODY = "BLOCK_BODY";
    private Map<String, PostDecodeStrategy> strategyMap = new HashMap<>();

    {
        strategyMap.put(BLOCK_BODY, new FormWithContentLengthStrategy());
        strategyMap.put(STREAM_BODY, new StreamWithContentLengthStrategy());
    }

    @Override
    public HttpV2Entity decode(ByteBuffer buffer, AioSession<HttpV2Entity> session) {
        HttpV2Entity entity = null;
        if (session.getAttachment() == null) {
            entity = new HttpV2Entity(session);
            session.setAttachment(entity);
        } else {
            entity = (HttpV2Entity) session.getAttachment();
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
                    break;
                }
                default: {
                    session.setAttachment(null);
                }
            }
        }
        if (entity.partFlag == HttpPart.END) {
            session.setAttachment(null);
        }
        return returnEntity ? entity : null;
    }

    @Override
    public ByteBuffer encode(HttpV2Entity httpEntity, AioSession<HttpV2Entity> session) {
        return null;
    }

    private void selectDecodeStrategy(HttpV2Entity entity) {
        if (entity.getContentLength() > 0) {
            if (entity.getContentLength() > 0 && StringUtils.startsWith(entity.getContentType(), "application/x-www-form-urlencoded")) {
                entity.postDecodeStrategy = strategyMap.get(BLOCK_BODY);
            } else {
                entity.postDecodeStrategy = strategyMap.get(STREAM_BODY);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        LOGGER.info(entity.postDecodeStrategy);
    }

}
