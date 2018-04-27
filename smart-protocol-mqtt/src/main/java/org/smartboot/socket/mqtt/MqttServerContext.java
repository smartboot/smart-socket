package org.smartboot.socket.mqtt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttServerContext implements MqttContext {
    private final ConcurrentMap<String, MqttSession> mqttSessionMap = new ConcurrentHashMap<>();

    @Override
    public MqttSession addSession(MqttSession session) {
        return mqttSessionMap.putIfAbsent(session.getClientId(), session);
    }
}
