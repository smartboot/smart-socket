package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.enums.MqttConnectReturnCode;
import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class ConnectProcessor implements MqttProcessor<MqttConnectMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectProcessor.class);

    @Override
    public void process(AioSession<MqttConnectMessage> aioSession, MqttConnectMessage mqttConnectMessage) {
        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        MqttConnAckMessage mqttConnAckMessage = MqttMessageBuilders.connAck()
                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                .sessionPresent(true).build();
        try {
            aioSession.write(mqttConnAckMessage.encode());
            LOGGER.info("response connect message:{}", mqttConnAckMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
