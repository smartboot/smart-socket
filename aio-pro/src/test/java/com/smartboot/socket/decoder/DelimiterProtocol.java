/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DelimiterProtocol.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket.decoder;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StringUtils;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class DelimiterProtocol implements Protocol<String> {

    //结束符\r\n
    private static final byte[] DELIMITER_BYTES = new byte[]{'\r', '\n'};

    @Override
    public String decode(ByteBuffer buffer, AioSession session) {
        DelimiterFrameDecoder delimiterFrameDecoder;
        if (session.getAttachment() == null) {//构造指定结束符的临时缓冲区
            delimiterFrameDecoder = new DelimiterFrameDecoder(DELIMITER_BYTES, 64);
            session.setAttachment(delimiterFrameDecoder);//缓存解码器已应对半包情况
        } else {
            delimiterFrameDecoder = session.getAttachment();
        }

        //未解析到DELIMITER_BYTES则返回null
        if (!delimiterFrameDecoder.decode(buffer)) {
            return null;
        }
        //解码成功
        ByteBuffer byteBuffer = delimiterFrameDecoder.getBuffer();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        session.setAttachment(null);//释放临时缓冲区
        return new String(bytes);
    }

    public static void main(String[] args) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(104857600);

        System.out.println(StringUtils.toHexString(b.array()));
    }
}
