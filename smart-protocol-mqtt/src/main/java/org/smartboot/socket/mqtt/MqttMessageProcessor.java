package org.smartboot.socket.mqtt;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.mqtt.message.MqttPingReqMessage;
import org.smartboot.socket.mqtt.message.MqttPingRespMessage;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttMessageProcessor implements MessageProcessor<MqttMessage> {
    @Override
    public void process(AioSession<MqttMessage> session, MqttMessage msg) {
        System.out.println(msg);
        if (msg instanceof MqttConnectMessage) {
            MqttConnAckMessage mqttConnAckMessage = MqttMessageBuilders.connAck()
                    .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                    .sessionPresent(true).build();
            try {
                session.write(mqttConnAckMessage.encode());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (msg instanceof MqttPingReqMessage) {

            MqttPingRespMessage mqttPingRespMessage = new MqttPingRespMessage();
            try {
                session.write(mqttPingRespMessage.encode());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stateEvent(AioSession<MqttMessage> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        System.out.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
