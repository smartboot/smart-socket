/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: LongProtocol.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example;

import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class LongProtocol implements Protocol<Long> {

    @Override
    public Long decode(ByteBuffer data, AioSession session) {
        if (data.remaining() < Long.BYTES)
            return null;
        return data.getLong();
    }
}