package org.smartboot.socket.extension.decoder.strategy;

import org.smartboot.socket.extension.decoder.HttpV2Entity;

import java.nio.ByteBuffer;

/**
 * 以流的形式传输并包含Content-Length的解码方式
 *
 * @author 三刀
 * @version V1.0 , 2017/9/3
 */
public class StreamWithContentLengthStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return false;
    }

    @Override
    public boolean isDecodeEnd(ByteBuffer buffer, HttpV2Entity entity, boolean eof) {
//        //识别body长度
//        if (entity.getContentLength() <= 0) {
//            throw new RuntimeException("invalid content length");
//        }
//        return entity.smartHttpInputStream.put(b);
        throw new UnsupportedOperationException();
    }

}
