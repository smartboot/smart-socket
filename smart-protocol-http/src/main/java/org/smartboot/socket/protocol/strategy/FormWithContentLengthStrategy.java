package org.smartboot.socket.protocol.strategy;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.protocol.FixedLengthFrameDecoder;
import org.smartboot.socket.protocol.HttpV2Entity;

import java.nio.ByteBuffer;

/**
 * Post普通表单提交
 *
 * @author 三刀
 * @version V1.0 , 2017/9/3
 */
public class FormWithContentLengthStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return true;
    }

    @Override
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
        //识别body长度
        if (entity.bodyContentDecoder == null) {
            entity.bodyContentDecoder = new FixedLengthFrameDecoder(entity.getContentLength());
        }
        if (entity.bodyContentDecoder.put(b)) {
            ByteBuffer contentBuffer = entity.bodyContentDecoder.getBuffer();
            String[] headDatas = StringUtils.split(new String(contentBuffer.array(), contentBuffer.position(), contentBuffer.remaining()), "&");
            if (ArrayUtils.isEmpty(headDatas)) {
                throw new RuntimeException("data is emtpy");
            }
            for (int i = 0; i < headDatas.length; i++) {
                entity.getParamMap().put(StringUtils.substringBefore(headDatas[i], "=").trim(), StringUtils.substringAfter(headDatas[i], "=").trim());
            }
            return true;
        }
        return false;
    }

}
