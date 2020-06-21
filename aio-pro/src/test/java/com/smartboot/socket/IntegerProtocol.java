/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerProtocol.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Created by 三刀 on 2018/08/23.
 */
public class IntegerProtocol implements Protocol<Integer> {

    private static final int INT_LENGTH = 4;

    @Override
    public Integer decode(ByteBuffer data, AioSession session) {
        if (data.remaining() < INT_LENGTH)
            return null;
        return data.getInt();
    }

}
