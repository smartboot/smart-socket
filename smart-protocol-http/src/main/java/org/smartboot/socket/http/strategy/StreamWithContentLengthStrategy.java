/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StreamWithContentLengthStrategy.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http.strategy;

import org.smartboot.socket.http.HttpV2Entity;

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
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
        //识别body长度
        if (entity.getContentLength() <= 0) {
            throw new RuntimeException("invalid content length");
        }
        return entity.smartHttpInputStream.append(b);
    }

}
