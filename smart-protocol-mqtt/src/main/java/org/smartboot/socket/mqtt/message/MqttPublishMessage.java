package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttPublishMessage extends MqttMessage {
    private MqttPublishVariableHeader mqttPublishVariableHeader;

    private ByteBuffer payload;

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader, ByteBuffer payload) {
        super(mqttFixedHeader);
        this.mqttPublishVariableHeader = mqttPublishVariableHeader;
        this.payload = payload;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final String decodedTopic = decodeString(buffer);
        if (!isValidPublishTopicName(decodedTopic)) {
            throw new DecoderException("invalid publish topic name: " + decodedTopic + " (contains wildcards)");
        }
        int messageId = -1;
        if (mqttFixedHeader.qosLevel().value() > 0) {
            messageId = decodeMessageId(buffer);
        }
        mqttPublishVariableHeader =
                new MqttPublishVariableHeader(decodedTopic, messageId);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        payload = ByteBuffer.allocate(buffer.remaining());
        payload.put(buffer);
    }
}
