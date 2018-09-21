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
    private static final int INT_BYTES = 4;//int类型的字节长度

    @Override
    public String decode(ByteBuffer readBuffer, AioSession<String> session) {
        if (session.getAttachment() == null && readBuffer.remaining() < INT_BYTES) {//首次解码不足四字节，无法知晓消息长度
            return null;
        }
        FixedLengthFrameDecoder fixedLengthFrameDecoder;
        if (session.getAttachment() != null) {
            fixedLengthFrameDecoder = session.getAttachment();
        } else {
            int length = readBuffer.getInt();//获得消息体长度
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

    @Override
    public ByteBuffer encode(String msg, AioSession<String> session) {
        byte[] bytes = msg.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(INT_BYTES + bytes.length);
        buffer.putInt(bytes.length);//消息头
        buffer.put(bytes);//消息体
        buffer.flip();
        return buffer;
    }
}
