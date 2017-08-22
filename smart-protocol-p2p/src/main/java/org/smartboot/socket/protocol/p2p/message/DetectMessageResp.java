package org.smartboot.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息响应
 *
 * @author Seer
 */
public class DetectMessageResp extends BaseMessage {

    public DetectMessageResp() {
        super();
    }

    public DetectMessageResp(HeadMessage head) {
        super(head);
    }

    private byte sendTime;

    public long getSendTime() {
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
        return MessageType.DETECT_MESSAGE_RSP;
    }

}
