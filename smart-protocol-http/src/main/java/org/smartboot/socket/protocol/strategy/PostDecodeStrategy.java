package org.smartboot.socket.protocol.strategy;

import org.smartboot.socket.protocol.HttpV2Entity;

/**
 * POST请求解码策略
 * @author Seer
 * @version V1.0 , 2017/9/3
 */
public interface PostDecodeStrategy {
    /**
     * 是否等待body解码完成才返回HTTP对象
     * @return
     */
    boolean waitForBodyFinish();

    boolean isDecodeEnd(byte b, HttpV2Entity httpV2Entity);
}
