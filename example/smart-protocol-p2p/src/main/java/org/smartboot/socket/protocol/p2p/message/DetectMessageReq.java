package org.smartboot.socket.protocol.p2p.message;

import java.nio.ByteBuffer;

/**
 * 探测消息请求
 *
 * @author 三刀
 */
public class DetectMessageReq extends BaseMessage {
    private String detect;

    public String getDetect() {
        return detect;
    }

    public void setDetect(String detect) {
        this.detect = detect;
    }

    @Override
    protected void encodeBody(ByteBuffer buffer) {
        writeString(buffer, detect);
    }

    @Override
    protected void decodeBody(ByteBuffer buffer) {
        detect = readString(buffer);
    }

    @Override
    public int getMessageType() {
        return MessageType.DETECT_MESSAGE_REQ;
    }

}
