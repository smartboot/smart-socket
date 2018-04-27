package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttMessageIdVariableHeader;
import org.smartboot.socket.mqtt.message.MqttSubAckMessage;
import org.smartboot.socket.mqtt.message.MqttSubAckPayload;
import org.smartboot.socket.mqtt.message.MqttSubscribeMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class SubscribeProcessor implements MqttProcessor<MqttSubscribeMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeProcessor.class);

    @Override
    public void process(MqttContext context, MqttSession session, MqttSubscribeMessage mqttSubscribeMessage) {
        LOGGER.info("receive subscribe message:{}", mqttSubscribeMessage);
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0));
        mqttSubAckMessage.setMqttMessageIdVariableHeader(MqttMessageIdVariableHeader.from(12345));
        mqttSubAckMessage.setMqttSubAckPayload(new MqttSubAckPayload(1, 2, 0));
        session.write(mqttSubAckMessage);
    }
}
