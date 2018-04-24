package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;

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

    @Override
    public final void decodeVariableHeader(ByteBuffer buffer) {
        final int messageId = decodeMessageId(buffer);
        mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
    }
}
