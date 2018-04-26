package org.smartboot.socket.mqtt.processor;

import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public interface MqttProcessor<T extends MqttMessage> {

    void process(AioSession<T> aioSession, T t);
}
