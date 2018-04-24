package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishVariableHeader {
    private final String topicName;
    private final int packetId;

    public MqttPublishVariableHeader(String topicName, int packetId) {
        this.topicName = topicName;
        this.packetId = packetId;
    }

    public String topicName() {
        return topicName;
    }

    /**
     * @deprecated Use {@link #packetId()} instead.
     */
    @Deprecated
    public int messageId() {
        return packetId;
    }

    public int packetId() {
        return packetId;
    }
}
