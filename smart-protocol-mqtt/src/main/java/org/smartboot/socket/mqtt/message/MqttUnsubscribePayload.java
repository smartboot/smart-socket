package org.smartboot.socket.mqtt.message;

import java.util.Collections;
import java.util.List;

public final class MqttUnsubscribePayload {

    private final List<String> topics;

    public MqttUnsubscribePayload(List<String> topics) {
        this.topics = Collections.unmodifiableList(topics);
    }

    public List<String> topics() {
        return topics;
    }


}