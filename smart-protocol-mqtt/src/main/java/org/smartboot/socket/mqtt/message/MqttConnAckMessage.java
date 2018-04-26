package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttConnectReturnCode;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckMessage extends MqttMessage {
    private MqttConnAckVariableHeader mqttConnAckVariableHeader;

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttConnAckMessage(MqttFixedHeader mqttFixedHeader, MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        super(mqttFixedHeader);
        this.mqttConnAckVariableHeader = mqttConnAckVariableHeader;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();
        mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
    }

    @Override
    public ByteBuffer encode() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(getFixedHeaderByte1(mqttFixedHeader));
        buffer.put((byte) 2);
        buffer.put((byte) (mqttConnAckVariableHeader.isSessionPresent() ? 0x01 : 0x00));
        buffer.put(mqttConnAckVariableHeader.connectReturnCode().getCode());
        buffer.flip();
        return buffer;
    }

    public MqttConnAckVariableHeader getMqttConnAckVariableHeader() {
        return mqttConnAckVariableHeader;
    }

    public void setMqttConnAckVariableHeader(MqttConnAckVariableHeader mqttConnAckVariableHeader) {
        this.mqttConnAckVariableHeader = mqttConnAckVariableHeader;
    }
}
