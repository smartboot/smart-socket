package org.smartboot.socket.mqtt.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttQoS {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2),
    FAILURE(0x80);

    private final int value;

    MqttQoS(int value) {
        this.value = value;
    }

    public static MqttQoS valueOf(int value) {
        for (MqttQoS q : values()) {
            if (q.value == value) {
                return q;
            }
        }
        throw new IllegalArgumentException("invalid QoS: " + value);
    }

    public int value() {
        return value;
    }
}
