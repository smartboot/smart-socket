package org.smartboot.socket.mqtt.message;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class OnlyFixedHeaderMessage extends MqttMessage {
    public OnlyFixedHeaderMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    public final ByteBuffer encode() {
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.put(getFixedHeaderByte1(mqttFixedHeader));
        buf.put((byte) 0);
        buf.flip();
        return buf;
    }
}
