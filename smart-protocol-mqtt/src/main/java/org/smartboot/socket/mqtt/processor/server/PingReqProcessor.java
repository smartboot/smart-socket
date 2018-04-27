package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.message.MqttPingReqMessage;
import org.smartboot.socket.mqtt.message.MqttPingRespMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

/**
 * 心跳请求处理
 *
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class PingReqProcessor implements MqttProcessor<MqttPingReqMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingReqProcessor.class);

    @Override
    public void process(MqttContext context, MqttSession session, MqttPingReqMessage msg) {
        LOGGER.info("receive ping req message:{}", msg);
        MqttPingRespMessage mqttPingRespMessage = new MqttPingRespMessage();
        session.write(mqttPingRespMessage);
        LOGGER.info("response ping message:{}", mqttPingRespMessage);
    }


}
