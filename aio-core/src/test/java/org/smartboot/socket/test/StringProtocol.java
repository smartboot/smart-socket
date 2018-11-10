package org.smartboot.socket.test;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.buffer.ByteBuf;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/25
 */
public class StringProtocol implements Protocol<String> {

    private static final int INT_LENGTH = 4;

    @Override
    public String decode(ByteBuffer readBuffer, AioSession<String> session) {
        //识别消息长度
        if (readBuffer.remaining() < INT_LENGTH) {
            return null;
        }
        //判断是否存在半包情况
        int len = readBuffer.getInt(0);
        if (readBuffer.remaining() < len) {
            return null;
        }
        readBuffer.getInt();//跳过length字段
        byte[] bytes = new byte[len - INT_LENGTH];
        readBuffer.get(bytes);
        return new String(bytes);
    }

    @Override
    public ByteBuf encode(String msg, AioSession<String> session) {
        byte[] bytes = msg.getBytes();
        ByteBuf buf = session.allocateBuf(INT_LENGTH + bytes.length);
        ByteBuffer buffer = buf.buffer();
        buffer.putInt(INT_LENGTH + bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buf;
    }
}
