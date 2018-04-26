package org.smartboot.socket.mqtt.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttMessageType {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14);

    private final int value;

    MqttMessageType(int value) {
        this.value = value;
    }

    public static MqttMessageType valueOf(int type) {
        for (MqttMessageType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown message type: " + type);
    }

    public int value() {
        return value;
    }
}
