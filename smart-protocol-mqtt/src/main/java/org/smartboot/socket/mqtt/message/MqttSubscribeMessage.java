package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubscribeMessage extends MessageIdVariableHeaderMessage {

    private MqttSubscribePayload mqttSubscribePayload;

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttSubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader, MqttSubscribePayload mqttSubscribePayload) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
        this.mqttSubscribePayload = mqttSubscribePayload;
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<MqttTopicSubscription> subscribeTopics = new ArrayList<MqttTopicSubscription>();
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            subscribeTopics.add(new MqttTopicSubscription(decodedTopicName, MqttQoS.valueOf(qos)));
        }
        this.mqttSubscribePayload = new MqttSubscribePayload(subscribeTopics);
    }


}
