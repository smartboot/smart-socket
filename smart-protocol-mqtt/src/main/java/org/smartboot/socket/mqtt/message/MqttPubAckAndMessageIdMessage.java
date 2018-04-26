package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPubAckAndMessageIdMessage extends SingleByteFixedHeaderAndMessageIdMessage {
    public MqttPubAckAndMessageIdMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }
}
