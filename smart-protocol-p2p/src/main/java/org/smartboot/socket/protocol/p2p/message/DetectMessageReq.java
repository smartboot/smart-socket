package org.smartboot.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息请求
 *
 * @author 三刀
 */
public class DetectMessageReq extends BaseMessage {
    private byte sendTime;

    public byte getSendTime() {
        return sendTime;
    }

    public void setSendTime(byte sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    protected void encodeBody(ByteBuffer buffer) {
        writeByte(buffer, sendTime);
    }

    @Override
    protected void decodeBody(ByteBuffer buffer) {
        sendTime = readByte(buffer);
    }

    @Override
    public int getMessageType() {
        return MessageType.DETECT_MESSAGE_REQ;
    }

}
