//package org.smartboot.socket.test;
//
//import org.smartboot.socket.Protocol;
//import org.smartboot.socket.buffer.ByteBuf;
//import org.smartboot.socket.transport.AioSession;
//
//import java.nio.ByteBuffer;
//
///**
// * Created by 三刀 on 2018/08/23.
// */
//public class IntegerProtocol implements Protocol<Integer> {
//
//    private static final int INT_LENGTH = 4;
//
//    @Override
//    public Integer decode(ByteBuffer data, AioSession<Integer> session) {
//        if (data.remaining() < INT_LENGTH)
//            return null;
//        return data.getInt();
//    }
//
//    @Override
//    public ByteBuf encode(Integer s, AioSession<Integer> session) {
//        ByteBuf byteBuf=session.allocateBuf(INT_LENGTH);
//        ByteBuffer b = byteBuf.buffer();
//        b.putInt(s);
//        b.flip();
//        return byteBuf;
//    }
//}
