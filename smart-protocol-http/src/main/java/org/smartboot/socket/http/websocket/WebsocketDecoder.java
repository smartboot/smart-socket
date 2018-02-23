/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WebsocketProtocol.java
 * Date: 2018-02-11
 * Author: sandao
 */

package org.smartboot.socket.http.websocket;

import org.smartboot.socket.http.HttpContentDecoder;
import org.smartboot.socket.http.HttpDecodeUnit;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/11
 */
public class WebsocketDecoder extends HttpContentDecoder {
    @Override
    public void decode(HttpDecodeUnit decodeUnit, ByteBuffer buffer) {
        throw new UnsupportedOperationException("unsupport websocket now!");
    }
}
