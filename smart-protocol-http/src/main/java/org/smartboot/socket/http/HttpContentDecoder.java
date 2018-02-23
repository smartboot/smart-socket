/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpContentDecoder.java
 * Date: 2018-02-16
 * Author: sandao
 */

package org.smartboot.socket.http;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public abstract class HttpContentDecoder {

    public abstract void decode(HttpDecodeUnit decodeUnit, ByteBuffer buffer);
}
