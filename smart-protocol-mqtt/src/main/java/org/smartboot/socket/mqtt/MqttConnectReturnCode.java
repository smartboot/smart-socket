package org.smartboot.socket.mqtt;

public enum MqttConnectReturnCode {
    CONNECTION_ACCEPTED((byte) 0x00),
    CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05);


    private final byte byteValue;

    MqttConnectReturnCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    public static MqttConnectReturnCode valueOf(byte b) {
        for (MqttConnectReturnCode v : values()) {
            if (b == v.byteValue) {
                return v;
            }
        }
        throw new IllegalArgumentException("unknown connect return code: " + (b & 0xFF));
    }

    public byte byteValue() {
        return byteValue;
    }
}