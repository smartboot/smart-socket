/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ByteArrayProtocol.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.extension.protocol;

import io.github.smartboot.socket.transport.AioSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/19
 */
public class ByteArrayProtocol extends FixedLengthBytesProtocol<byte[]> {
    @Override
    protected byte[] decode(byte[] bytes, AioSession session) {
        return bytes;
    }
}
