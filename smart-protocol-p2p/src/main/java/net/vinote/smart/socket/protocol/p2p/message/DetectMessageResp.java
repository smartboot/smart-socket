package net.vinote.smart.socket.protocol.p2p.message;

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

    private String detectMessage;

    private long sendTime;

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    protected void encodeBody(ByteBuffer buffer) {
        writeString(buffer, detectMessage);
        writeLong(buffer, sendTime);
    }

    protected void decodeBody(ByteBuffer buffer) {
        detectMessage = readString(buffer);
        sendTime = readLong(buffer);
    }

    public int getMessageType() {
        return MessageType.DETECT_MESSAGE_RSP;
    }

    public String getDetectMessage() {
        return detectMessage;
    }

    public void setDetectMessage(String detectMessage) {
        this.detectMessage = detectMessage;
    }
}
