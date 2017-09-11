package org.smartboot.socket.protocol.strategy;

import org.smartboot.socket.protocol.HttpV2Entity;

/**
 * 以流的形式传输并包含Content-Length的解码方式
 *
 * @author Seer
 * @version V1.0 , 2017/9/3
 */
public class StreamWithContentLengthStrategy implements PostDecodeStrategy {
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
