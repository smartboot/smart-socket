/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FixedLengthProtocol.java
 * Date: 2021-05-03
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.protocol;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/19
 */
public abstract class FixedLengthBytesProtocol<T> implements Protocol<T> {
    @Override
    public final T decode(ByteBuffer readBuffer, AioSession session) {
        if (readBuffer.remaining() < Integer.BYTES) {
            return null;
        }
        readBuffer.mark();
        int length = readBuffer.getInt();
        if (readBuffer.remaining() < length) {
            readBuffer.reset();
            return null;
        }
        byte[] bytes = new byte[length];
        readBuffer.get(bytes);
        return decode(bytes, session);
    }

    protected abstract T decode(byte[] bytes, AioSession session);
}
