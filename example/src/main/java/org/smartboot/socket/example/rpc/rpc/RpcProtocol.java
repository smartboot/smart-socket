/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RpcProtocol.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.rpc.rpc;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class RpcProtocol implements Protocol<byte[]> {
    private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;

    @Override
    public byte[] decode(ByteBuffer readBuffer, AioSession session) {
        int remaining = readBuffer.remaining();
        if (remaining < INTEGER_BYTES) {
            return null;
        }
        int messageSize = readBuffer.getInt(readBuffer.position());
        if (messageSize > remaining) {
            return null;
        }
        byte[] data = new byte[messageSize - INTEGER_BYTES];
        readBuffer.getInt();
        readBuffer.get(data);
        return data;
    }
}
