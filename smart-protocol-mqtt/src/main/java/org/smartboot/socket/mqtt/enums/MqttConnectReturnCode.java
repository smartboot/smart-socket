package org.smartboot.socket.mqtt.enums;

/**
 *
 */
public enum MqttConnectReturnCode {
    CONNECTION_ACCEPTED((byte) 0x00, "连接已被服务端接受"),
    CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01, "服务端不支持客户端请求的 MQTT 协议级别"),
    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02, "客户端标识符是正确的 UTF-8 编码，但服务 端不允许使用"),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03, "网络连接已建立，但 MQTT 服务不可用"),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04, "用户名或密码的数据格式无效"),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05, "客户端未被授权连接到此服务器");


    private final byte code;
    private final String desc;

    MqttConnectReturnCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MqttConnectReturnCode valueOf(byte b) {
        for (MqttConnectReturnCode v : values()) {
            if (b == v.code) {
                return v;
            }
        }
        throw new IllegalArgumentException("unknown connect return code: " + (b & 0xFF));
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}