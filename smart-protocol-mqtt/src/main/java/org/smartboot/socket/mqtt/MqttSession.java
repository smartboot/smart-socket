package org.smartboot.socket.mqtt;

import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/26
 */
public class MqttSession {
    private String clientId;
    private boolean cleanSession;
    private AioSession<MqttMessage> session;

    public MqttSession(AioSession<MqttMessage> session) {
        this.session = session;
    }

    public void init(String clientId, boolean cleanSession) {
        this.clientId = clientId;
        this.cleanSession = cleanSession;
    }

    public void write(MqttMessage mqttMessage) {
        try {
            session.write(mqttMessage.encode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        session.close(false);
    }

    public String getClientId() {
        return clientId;
    }

}
