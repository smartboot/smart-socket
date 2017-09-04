package org.smartboot.socket.protocol.strategy;

import org.smartboot.socket.protocol.HttpV2Entity;

/**
 * Post普通表单提交
 *
 * @author Seer
 * @version V1.0 , 2017/9/3
 */
public class FormDataBoundaryStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return false;
    }

    @Override
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
        //识别body长度
        if (entity.getContentLength() <= 0) {
            throw new RuntimeException("invalid content length");
        }
        try {
            entity.binaryBuffer.put(b);
        } catch (InterruptedException e) {
            throw new RuntimeException("invalid content length");
        }
        entity.binReadLength++;
        return entity.getContentLength() == entity.binReadLength;
    }

}
