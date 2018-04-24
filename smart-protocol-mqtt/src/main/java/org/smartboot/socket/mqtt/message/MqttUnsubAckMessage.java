package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttUnsubAckMessage extends MessageIdVariableHeaderMessage {
    public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }
}
