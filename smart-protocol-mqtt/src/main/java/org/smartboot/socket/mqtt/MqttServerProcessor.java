package org.smartboot.socket.mqtt;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.mqtt.message.MqttPingReqMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.message.MqttSubscribeMessage;
import org.smartboot.socket.mqtt.processor.MqttProcessor;
import org.smartboot.socket.mqtt.processor.server.ConnectProcessor;
import org.smartboot.socket.mqtt.processor.server.PingReqProcessor;
import org.smartboot.socket.mqtt.processor.server.PublishProcessor;
import org.smartboot.socket.mqtt.processor.server.SubscribeProcessor;
import org.smartboot.socket.transport.AioSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttServerProcessor implements MessageProcessor<MqttMessage> {
    private Map<Class<? extends MqttMessage>, MqttProcessor> processorMap = new HashMap<>();
    private MqttContext mqttContext = new MqttServerContext();
    private Map<String, MqttSession> sessionMap = new ConcurrentHashMap();

    {
        processorMap.put(MqttPingReqMessage.class, new PingReqProcessor());
        processorMap.put(MqttConnectMessage.class, new ConnectProcessor());
        processorMap.put(MqttPublishMessage.class, new PublishProcessor());
        processorMap.put(MqttSubscribeMessage.class, new SubscribeProcessor());
    }


    @Override
    public void process(AioSession<MqttMessage> session, MqttMessage msg) {
        MqttProcessor processor = processorMap.get(msg.getClass());
        if (processor != null) {
            processor.process(mqttContext, sessionMap.get(session.getSessionID()), msg);
        } else {
            System.out.println(msg);
        }
    }

    @Override
    public void stateEvent(AioSession<MqttMessage> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                sessionMap.put(session.getSessionID(), new MqttSession(session));
                break;
        }
        System.out.println(stateMachineEnum);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
