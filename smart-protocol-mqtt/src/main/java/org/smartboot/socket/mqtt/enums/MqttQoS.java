package org.smartboot.socket.mqtt.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public enum MqttQoS {
    AT_MOST_ONCE(0, "最多分发一次"),
    AT_LEAST_ONCE(1, "至少分发一次"),
    EXACTLY_ONCE(2, "只分发一次"),
    FAILURE(0x80, "暂不支持");

    private final int value;

    private final String desc;

    MqttQoS(int value, String desc) {
        this.value = value;
        this.desc = desc;
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
