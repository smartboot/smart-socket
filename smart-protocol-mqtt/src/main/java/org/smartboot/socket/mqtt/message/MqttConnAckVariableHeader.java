package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.enums.MqttConnectReturnCode;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnAckVariableHeader {
    private final MqttConnectReturnCode connectReturnCode;

    private final boolean sessionPresent;

    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
    }

    public MqttConnectReturnCode connectReturnCode() {
        return connectReturnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }
}
