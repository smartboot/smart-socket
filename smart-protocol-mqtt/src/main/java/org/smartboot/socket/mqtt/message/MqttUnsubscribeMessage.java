package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<String> unsubscribeTopics = new ArrayList<String>();
        while (buffer.hasRemaining()) {
            final String decodedTopicName = decodeString(buffer);
            unsubscribeTopics.add(decodedTopicName);
        }
        mqttUnsubscribePayload = new MqttUnsubscribePayload(unsubscribeTopics);
    }

    public final class MqttUnsubscribePayload {

        private final List<String> topics;

        public MqttUnsubscribePayload(List<String> topics) {
            this.topics = Collections.unmodifiableList(topics);
        }

        public List<String> topics() {
            return topics;
        }


    }
}
