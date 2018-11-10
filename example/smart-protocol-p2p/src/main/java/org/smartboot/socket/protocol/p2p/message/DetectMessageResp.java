package org.smartboot.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息响应
 *
 * @author 三刀
 */
public class DetectMessageResp extends BaseMessage {

    private long sendTime;

    public DetectMessageResp() {
        super();
    }

    public DetectMessageResp(HeadMessage head) {
        super(head);
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    protected void encodeBody(ByteBuffer buffer) {
        writeLong(buffer, sendTime);
    }

    @Override
    protected void decodeBody(ByteBuffer buffer) {
        sendTime = readLong(buffer);
    }

    @Override
    public int getMessageType() {
        return MessageType.DETECT_MESSAGE_RSP;
    }

}
