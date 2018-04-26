package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttInvalidMessage extends MqttMessage{
    public MqttInvalidMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

}
