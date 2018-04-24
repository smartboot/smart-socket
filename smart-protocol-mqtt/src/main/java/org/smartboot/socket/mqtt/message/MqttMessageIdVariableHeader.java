package org.smartboot.socket.mqtt.message;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttMessageIdVariableHeader {
    private final int messageId;

    public static MqttMessageIdVariableHeader from(int messageId) {
        if (messageId < 1 || messageId > 0xffff) {
            throw new IllegalArgumentException("messageId: " + messageId + " (expected: 1 ~ 65535)");
        }
        return new MqttMessageIdVariableHeader(messageId);
    }

    private MqttMessageIdVariableHeader(int messageId) {
        this.messageId = messageId;
    }

    public int messageId() {
        return messageId;
    }
}
