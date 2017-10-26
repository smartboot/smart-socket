package org.smartboot.socket.protocol.http;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
public class HttpProtocol implements Protocol<HttpEntity> {

    @Override
    public HttpEntity decode(ByteBuffer buffer, AioSession<HttpEntity> session, boolean eof) {
        HttpEntity entity = null;
        if (session.getAttachment() == null) {
            entity = new HttpEntity();
            session.setAttachment(entity);
        } else {
            entity = (HttpEntity) session.getAttachment();
        }
        boolean returnEntity = false;//是否返回HttpEntity
        switch (entity.getDecodePart()) {
            case HEAD:
                if (entity.delimiterFrameDecoder.decoder(buffer)) {
                    entity.decodeHead();//消息头解码
                    if (entity.getDecodePart() == HttpDecodePart.END) {
                        returnEntity = true;
                        session.setAttachment(null);
                        break;
                    } else if (entity.getDecodePart() == HttpDecodePart.BODY) {
                        returnEntity = !entity.postDecodeStrategy.waitForBodyFinish();
                    }
                } else {
                    break;
                }
            case BODY:
                if (entity.postDecodeStrategy.isDecodeEnd(buffer, entity, eof)) {
                    entity.setDecodePart(HttpDecodePart.END);
                    returnEntity = entity.postDecodeStrategy.waitForBodyFinish();
                    break;
                }
                break;
            default:
                session.setAttachment(null);
        }
        return returnEntity ? entity : null;
    }

    @Override
    public ByteBuffer encode(HttpEntity httpEntity, AioSession<HttpEntity> session) {
        return null;
    }


}
