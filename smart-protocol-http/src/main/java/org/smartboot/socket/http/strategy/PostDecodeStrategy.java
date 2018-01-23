/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: PostDecodeStrategy.java
 * Date: 2018-01-23 16:28:22
 * Author: sandao
 */

package org.smartboot.socket.http.strategy;


import org.smartboot.socket.http.HttpV2Entity;

/**
 * POST请求解码策略
 * @author 三刀
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
