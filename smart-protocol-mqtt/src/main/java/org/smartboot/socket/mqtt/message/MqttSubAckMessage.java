package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttSubAckMessage extends MessageIdVariableHeaderMessage {
    private MqttSubAckPayload mqttSubAckPayload;

    public MqttSubAckMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final List<Integer> grantedQos = new ArrayList<Integer>();
        while (buffer.hasRemaining()) {
            int qos = BufferUtils.readUnsignedByte(buffer) & 0x03;
            grantedQos.add(qos);
        }
        mqttSubAckPayload = new MqttSubAckPayload(grantedQos);
    }

    class MqttSubAckPayload {

        private final List<Integer> grantedQoSLevels;

        public MqttSubAckPayload(int... grantedQoSLevels) {
            if (grantedQoSLevels == null) {
                throw new NullPointerException("grantedQoSLevels");
            }

            List<Integer> list = new ArrayList<Integer>(grantedQoSLevels.length);
            for (int v : grantedQoSLevels) {
                list.add(v);
            }
            this.grantedQoSLevels = Collections.unmodifiableList(list);
        }

        public MqttSubAckPayload(Iterable<Integer> grantedQoSLevels) {
            if (grantedQoSLevels == null) {
                throw new NullPointerException("grantedQoSLevels");
            }
            List<Integer> list = new ArrayList<Integer>();
            for (Integer v : grantedQoSLevels) {
                if (v == null) {
                    break;
                }
                list.add(v);
            }
            this.grantedQoSLevels = Collections.unmodifiableList(list);
        }

        public List<Integer> grantedQoSLevels() {
            return grantedQoSLevels;
        }

    }
}
