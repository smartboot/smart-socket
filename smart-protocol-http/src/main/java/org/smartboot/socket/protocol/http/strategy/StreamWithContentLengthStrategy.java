package org.smartboot.socket.protocol.http.strategy;

import org.smartboot.socket.protocol.http.HttpEntity;

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
    public boolean isDecodeEnd(ByteBuffer buffer, HttpEntity entity, boolean eof) {
        //识别body长度
        if (entity.getContentLength() <= 0) {
            throw new RuntimeException("invalid content length");
        }
        if (eof) {
            while (buffer.hasRemaining() && !entity.smartHttpInputStream.put(buffer)) ;
            return true;
        }
        return entity.smartHttpInputStream.put(buffer);
    }

}
