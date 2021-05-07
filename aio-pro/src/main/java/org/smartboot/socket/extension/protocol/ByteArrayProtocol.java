/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ByteArrayProtocol.java
 * Date: 2021-05-03
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.protocol;

import org.smartboot.socket.transport.AioSession;

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
