package org.smartboot.socket.mqtt.message;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttFixedHeader {

    private final MqttMessageType messageType;
    /**
     * 重发标志
     */
    private final boolean dup;
    private final MqttQoS qosLevel;
    /**
     * 保留标志
     */
    private final boolean retain;
    private final int remainingLength;

    public MqttFixedHeader(
            MqttMessageType messageType,
            boolean dup,
            MqttQoS qosLevel,
            boolean retain,
            int remainingLength) {
        this.messageType = messageType;
        this.dup = dup;
        this.qosLevel = qosLevel;
        this.retain = retain;
        this.remainingLength = remainingLength;
    }

    public MqttMessageType messageType() {
        return messageType;
    }

    public boolean isDup() {
        return dup;
    }

    public MqttQoS qosLevel() {
        return qosLevel;
    }

    public boolean isRetain() {
        return retain;
    }

    public int remainingLength() {
        return remainingLength;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
