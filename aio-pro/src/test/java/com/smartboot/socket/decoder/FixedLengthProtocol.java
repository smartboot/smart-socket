/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FixedLengthProtocol.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket.decoder;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * s
 *
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class FixedLengthProtocol implements Protocol<String> {

    @Override
    public String decode(ByteBuffer readBuffer, AioSession session) {
        if (!readBuffer.hasRemaining()) {
            return null;
        }
        FixedLengthFrameDecoder fixedLengthFrameDecoder;
        if (session.getAttachment() != null) {
            fixedLengthFrameDecoder = session.getAttachment();
        } else {
            byte length = readBuffer.get();//获得消息体长度
            fixedLengthFrameDecoder = new FixedLengthFrameDecoder(length);//构建指定长度的临时缓冲区
            session.setAttachment(fixedLengthFrameDecoder);//缓存临时缓冲区
        }

        if (!fixedLengthFrameDecoder.decode(readBuffer)) {
            return null;//已读取的数据不足length，返回null
        }
        //数据读取完毕
        ByteBuffer fullBuffer = fixedLengthFrameDecoder.getBuffer();
        byte[] bytes = new byte[fullBuffer.remaining()];
        fullBuffer.get(bytes);
        session.setAttachment(null);//释放临时缓冲区
        return new String(bytes);
    }
}
