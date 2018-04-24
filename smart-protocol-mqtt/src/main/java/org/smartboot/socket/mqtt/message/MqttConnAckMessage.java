package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttConnectReturnCode;
import org.smartboot.socket.mqtt.MqttFixedHeader;
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

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final boolean sessionPresent = (BufferUtils.readUnsignedByte(buffer) & 0x01) == 0x01;
        byte returnCode = buffer.get();
        mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
    }
}
