/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SmartDecoder.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.socket.extension.decoder;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/28
 */
public interface SmartDecoder {
    /**
     * 解码算法
     *
     * @param byteBuffer
     * @return
     */
    boolean decode(ByteBuffer byteBuffer);

    /**
     * 获取本次解析到的完整数据
     *
     * @return
     */
    ByteBuffer getBuffer();
}
