package org.smartboot.socket.extension.decoder.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息响应
 *
 * @author 三刀
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
        return MessageType.DETECT_MESSAGE_RSP;
    }

}
