package org.smartboot.socket.transport.kcp;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

public class KcpProtocol implements Protocol<KcpPacket> {
    @Override
    public KcpPacket decode(ByteBuffer readBuffer, AioSession session) {
        if (!readBuffer.hasRemaining()) {
            return null;
        }
        KcpPacket packet = new KcpPacket();
        return packet;
    }
}
