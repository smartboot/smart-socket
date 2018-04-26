package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SingleByteFixedHeaderAndMessageIdMessage extends MessageIdVariableHeaderMessage {
    public SingleByteFixedHeaderAndMessageIdMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public ByteBuffer encode() {
        int msgId = mqttMessageIdVariableHeader.messageId();

        int variableHeaderBufferSize = 2; // variable part only has a message id
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variableHeaderBufferSize);
        buf.put(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variableHeaderBufferSize);
        buf.putShort((short) msgId);
        buf.flip();
        return buf;
    }
}
