package net.vinote.demo;

import org.smartboot.socket.protocol.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Created by zhengjunwei on 2017/7/12.
 */
public class SimpleProtocol implements Protocol<String> {
    @Override
    public String decode(ByteBuffer data, AioSession<String> session) {
        if (data.remaining() < 2)
            return null;
        for (int i = 1; i < data.remaining(); i++) {
            if (data.get(i) == '\n' && data.get(i - 1) == '\r') {
                byte[] d = new byte[i+1];
                data.get(d);
//                System.out.println("decode");
                return new String(d);
            }
        }
        return null;
    }

    @Override
    public ByteBuffer encode(String s, AioSession<String> session) {
        ByteBuffer b=ByteBuffer.wrap(s.getBytes());
        b.position(b.limit());
        return b;
    }
}
