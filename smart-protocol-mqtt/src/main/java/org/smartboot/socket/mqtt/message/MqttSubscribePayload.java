package org.smartboot.socket.mqtt.message;

import java.util.Collections;
import java.util.List;

public final class MqttSubscribePayload {

    private final List<MqttTopicSubscription> topicSubscriptions;

    public MqttSubscribePayload(List<MqttTopicSubscription> topicSubscriptions) {
        this.topicSubscriptions = Collections.unmodifiableList(topicSubscriptions);
    }

    public List<MqttTopicSubscription> topicSubscriptions() {
        return topicSubscriptions;
    }

}