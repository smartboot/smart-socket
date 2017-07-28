package net.vinote.smart.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息请求
 *
 * @author Seer
 */
public class DetectMessageReq extends BaseMessage {
    private byte sendTime;

    public byte getSendTime() {
        return sendTime;
    }

    public void setSendTime(byte sendTime) {
        this.sendTime = sendTime;
    }

    protected void encodeBody(ByteBuffer buffer) {
        writeByte(buffer, sendTime);
    }

    protected void decodeBody(ByteBuffer buffer) {
        sendTime = readByte(buffer);
    }

    public int getMessageType() {
        return MessageType.DETECT_MESSAGE_REQ;
    }

}
