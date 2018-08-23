package org.smartboot.socket.protocol.p2p.message;

import org.smartboot.socket.protocol.p2p.DecodeException;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/23
 */
public class HeartMessageRsp extends BaseMessage {
    @Override
    protected void encodeBody(ByteBuffer buffer) throws ProtocolException {

    }

    @Override
    protected void decodeBody(ByteBuffer buffer) throws DecodeException {

    }

    @Override
    public int getMessageType() {
        return MessageType.HEART_MESSAGE_RSP;
    }
}
