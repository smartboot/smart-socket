package org.smartboot.socket.mqtt.enums;

import org.smartboot.socket.mqtt.exception.MqttUnacceptableProtocolVersionException;

import java.nio.charset.Charset;

public enum MqttVersion {
    MQTT_3_1("MQIsdp", (byte) 3),
    MQTT_3_1_1("MQTT", (byte) 4);

    private final String name;
    private final byte level;

    MqttVersion(String protocolName, byte protocolLevel) {
        name = protocolName;
        level = protocolLevel;
    }

    public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel) {
        for (MqttVersion mv : values()) {
            if (mv.name.equals(protocolName)) {
                if (mv.level == protocolLevel) {
                    return mv;
                } else {
                    throw new MqttUnacceptableProtocolVersionException(protocolName + " and " +
                            protocolLevel + " are not match");
                }
            }
        }
        throw new MqttUnacceptableProtocolVersionException(protocolName + "is unknown protocol name");
    }

    public String protocolName() {
        return name;
    }

    public byte[] protocolNameBytes() {
        return name.getBytes(Charset.forName("utf8"));
    }

    public byte protocolLevel() {
        return level;
    }
}
