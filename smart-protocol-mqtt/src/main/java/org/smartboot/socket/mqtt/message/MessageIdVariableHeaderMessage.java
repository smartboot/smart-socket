package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MessageIdVariableHeaderMessage extends MqttMessage {
    protected MqttMessageIdVariableHeader mqttMessageIdVariableHeader;

    public MessageIdVariableHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MessageIdVariableHeaderMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        super(mqttFixedHeader);
        this.mqttMessageIdVariableHeader = mqttMessageIdVariableHeader;
    }

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
        final int messageId = decodeMessageId(buffer);
        mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
    }

    public void setMqttMessageIdVariableHeader(MqttMessageIdVariableHeader mqttMessageIdVariableHeader) {
        this.mqttMessageIdVariableHeader = mqttMessageIdVariableHeader;
    }
}
