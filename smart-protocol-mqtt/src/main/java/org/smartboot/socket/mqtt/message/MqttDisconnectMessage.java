package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttDisconnectMessage extends OnlyFixedHeaderMessage {
    public MqttDisconnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }
}