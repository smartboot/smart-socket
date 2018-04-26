package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.message.MqttPingReqMessage;
import org.smartboot.socket.mqtt.message.MqttPingRespMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * 心跳请求处理
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PingReqProcessor implements MqttProcessor<MqttPingReqMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingReqProcessor.class);

    @Override
    public void process(AioSession<MqttPingReqMessage> session, MqttPingReqMessage msg) {
        LOGGER.info("receive ping req message:{}", msg);
        MqttPingRespMessage mqttPingRespMessage = new MqttPingRespMessage();
        try {
            session.write(mqttPingRespMessage.encode());
            LOGGER.info("response ping message:{}", mqttPingRespMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
