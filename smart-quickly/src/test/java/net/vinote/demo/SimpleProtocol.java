package net.vinote.demo;

import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.io.Channel;

import java.nio.ByteBuffer;

/**
 * Created by zhengjunwei on 2017/7/12.
 */
public class SimpleProtocol implements Protocol<String> {
    @Override
    public String decode(ByteBuffer data, Channel<String> session) {
        if (data.remaining() < 2)
            return null;
        for (int i = 1; i < data.remaining(); i++) {
            if (data.get(i) == '\n' && data.get(i - 1) == '\r') {
                byte[] d = new byte[i];
                data.get(d);
                return new String(d);
            }
        }
        return null;
    }

    @Override
    public ByteBuffer encode(String s, Channel<String> session) {
        return null;
    }
}
