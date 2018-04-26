package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MessageIdVariableHeaderMessage {
    private MqttSubAckPayload mqttSubAckPayload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<Integer> grantedQos = new ArrayList<Integer>();
        while (buffer.hasRemaining()) {
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            grantedQos.add(qos);
        }
        mqttSubAckPayload = new MqttSubAckPayload(grantedQos);
    }

    @Override
    public ByteBuffer encode() {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = mqttSubAckPayload.grantedQoSLevels().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put(getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variablePartSize);
        buf.putShort((short) mqttMessageIdVariableHeader.messageId());
        for (int qos : mqttSubAckPayload.grantedQoSLevels()) {
            buf.put((byte) qos);
        }
        buf.flip();
        return buf;
    }

    public void setMqttSubAckPayload(MqttSubAckPayload mqttSubAckPayload) {
        this.mqttSubAckPayload = mqttSubAckPayload;
    }
}
