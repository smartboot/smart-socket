package org.smartboot.socket.extension.decoder;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.strategy.FormWithContentLengthStrategy;
import org.smartboot.socket.extension.decoder.strategy.PostDecodeStrategy;
import org.smartboot.socket.extension.decoder.strategy.StreamWithContentLengthStrategy;
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
    public HttpV2Entity decode(ByteBuffer buffer, AioSession<HttpV2Entity> session, boolean eof) {
        HttpV2Entity entity = null;
        if (session.getAttachment() == null) {
            entity = new HttpV2Entity(session);
            session.setAttachment(entity);
        } else {
            entity = (HttpV2Entity) session.getAttachment();
        }
        boolean returnEntity = false;//是否返回HttpEntity
        switch (entity.partFlag) {
            case HEAD:
                while (buffer.hasRemaining()) {
                    if (entity.headDelimiterFrameDecoder.put(buffer.get())) {
                        entity.decodeHead();//消息头解码
                        if (StringUtils.equalsIgnoreCase("POST", entity.getMethod()) && entity.getContentLength() != 0) {
                            entity.partFlag = HttpPart.BODY;
                            selectDecodeStrategy(entity);//识别body解码处理器
                            returnEntity = !entity.postDecodeStrategy.waitForBodyFinish();
                        } else {
                            entity.partFlag = HttpPart.END;
                            returnEntity = true;
                        }
                        break;
                    }
                }
                if (entity.partFlag != HttpPart.BODY) {
                    break;
                }
            case BODY:
                if (entity.postDecodeStrategy.isDecodeEnd(buffer, entity, eof)) {
                    entity.partFlag = HttpPart.END;
                    returnEntity = entity.postDecodeStrategy.waitForBodyFinish();
                    break;
                }
                break;
            default:
                session.setAttachment(null);
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
