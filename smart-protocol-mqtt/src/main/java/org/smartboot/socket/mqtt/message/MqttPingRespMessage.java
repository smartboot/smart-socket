package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPingRespMessage extends OnlyFixedHeaderMessage {
    public MqttPingRespMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPingRespMessage() {
        super(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}