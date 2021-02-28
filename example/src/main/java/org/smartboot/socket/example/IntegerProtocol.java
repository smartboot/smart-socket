/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerProtocol.java
 * Date: 2021-02-28
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class IntegerProtocol implements Protocol<Integer> {

    @Override
    public Integer decode(ByteBuffer data, AioSession session) {
        if (data.remaining() < Integer.BYTES)
            return null;
        return data.getInt();
    }
}