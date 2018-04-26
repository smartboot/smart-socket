package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubscribeMessage extends MessageIdVariableHeaderMessage {
    private MqttUnsubscribePayload mqttUnsubscribePayload;

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttUnsubscribeMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader mqttMessageIdVariableHeader, MqttUnsubscribePayload mqttUnsubscribePayload) {
        super(mqttFixedHeader, mqttMessageIdVariableHeader);
        this.mqttUnsubscribePayload = mqttUnsubscribePayload;
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<String> unsubscribeTopics = new ArrayList<String>();
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            unsubscribeTopics.add(decodedTopicName);
        }
        mqttUnsubscribePayload = new MqttUnsubscribePayload(unsubscribeTopics);
    }


}
