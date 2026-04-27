/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: KcpProtocol.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.transport.kcp;

import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class KcpProtocol implements Protocol<KcpPacket> {
    @Override
    public KcpPacket decode(ByteBuffer readBuffer, AioSession session) {
        if (!readBuffer.hasRemaining()) {
            return null;
        }
        KcpPacket packet = new KcpPacket();
        return packet;
    }
}
