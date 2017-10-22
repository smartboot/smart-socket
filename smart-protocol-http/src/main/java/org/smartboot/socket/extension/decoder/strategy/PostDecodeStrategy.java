package org.smartboot.socket.extension.decoder.strategy;

import org.smartboot.socket.extension.decoder.HttpV2Entity;

import java.nio.ByteBuffer;

/**
 * POST请求解码策略
 *
 * @author 三刀
 * @version V1.0 , 2017/9/3
 */
public interface PostDecodeStrategy {
    /**
     * 是否等待body解码完成才返回HTTP对象
     *
     * @return
     */
    boolean waitForBodyFinish();

    boolean isDecodeEnd(ByteBuffer buffer, HttpV2Entity httpV2Entity, boolean eof);
}
