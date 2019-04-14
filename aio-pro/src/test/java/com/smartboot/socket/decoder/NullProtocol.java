package com.smartboot.socket.decoder;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/3/27
 */
public class NullProtocol implements Protocol<Void> {
    @Override
    public Void decode(ByteBuffer readBuffer, AioSession<Void> session) {
        return null;
    }

    @Override
    public ByteBuffer encode(Void msg, AioSession<Void> session) {
        return null;
    }
}
