package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;
import org.smartboot.socket.mqtt.MqttMessageType;
import org.smartboot.socket.mqtt.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPingRespMessage extends OnlyFixedHeaderMessage {
    public MqttPingRespMessage() {
        super(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}