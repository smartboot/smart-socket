/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StringProtocol.java
 * Date: 2021-05-03
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.protocol;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringProtocol implements Protocol<String> {

    private static final AttachKey<FixedLengthFrameDecoder> FRAME_DECODER_ATTACH_KEY = AttachKey.valueOf("fixedDecoder");
    private final Charset charset;

    public StringProtocol(Charset charset) {
        this.charset = charset;
    }

    public StringProtocol() {
        this(StandardCharsets.UTF_8);
    }

    @Override
    public String decode(ByteBuffer readBuffer, AioSession session) {
        Attachment attachment = session.getAttachment();
        FixedLengthFrameDecoder decoder = attachment.get(FRAME_DECODER_ATTACH_KEY);
        //消息长度超过缓冲区容量
        if (decoder != null) {
            String content = bigContent(readBuffer, decoder);
            //解码成功,释放解码器
            if (content != null) {
                attachment.remove(FRAME_DECODER_ATTACH_KEY);
            }
            return content;
        }

        int remaining = readBuffer.remaining();
        if (remaining < Integer.BYTES) {
            return null;
        }
        readBuffer.mark();
        int length = readBuffer.getInt();
        //消息长度超过缓冲区容量引发的半包,启用定长消息解码器,本次解码失败
        if (length + Integer.BYTES > readBuffer.capacity()) {
            FixedLengthFrameDecoder fixedLengthFrameDecoder = new FixedLengthFrameDecoder(length);
            attachment.put(FRAME_DECODER_ATTACH_KEY, fixedLengthFrameDecoder);
            return null;
        }
        //半包，解码失败
        if (length > readBuffer.remaining()) {
            readBuffer.reset();
            return null;
        }
        return convert(readBuffer, length);
    }

    /**
     * 大消息体解码
     */
    private String bigContent(ByteBuffer readBuffer, FixedLengthFrameDecoder decoder) {
        if (!decoder.decode(readBuffer)) {
            return null;
        }
        ByteBuffer byteBuffer = decoder.getBuffer();
        return convert(byteBuffer, byteBuffer.capacity());
    }

    /**
     * 消息解码
     */
    private String convert(ByteBuffer byteBuffer, int length) {
        byte[] b = new byte[length];
        byteBuffer.get(b);
        return new String(b, charset);
    }
}
