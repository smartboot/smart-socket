/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerProtocol.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example;

import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class IntegerProtocol implements Protocol<Integer> {

    @Override
    public Integer decode(ByteBuffer data, AioSession session) {
        if (data.remaining() < Integer.BYTES)
            return null;
        return data.getInt();
    }
}