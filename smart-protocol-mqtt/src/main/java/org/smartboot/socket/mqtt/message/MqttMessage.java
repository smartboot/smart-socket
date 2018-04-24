package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;
import org.smartboot.socket.mqtt.VariableHeader;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttMessage implements VariableHeader {
    /**
     * 8-bit UTF (UCS Transformation Format)
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};
    protected MqttFixedHeader mqttFixedHeader;

    public MqttMessage(MqttFixedHeader mqttFixedHeader) {
        this.mqttFixedHeader = mqttFixedHeader;
    }

    protected final String decodeString(ByteBuffer buffer) {
        return decodeString(buffer, 0, Integer.MAX_VALUE);
    }

    protected final String decodeString(ByteBuffer buffer, int minBytes, int maxBytes) {
        final int size = decodeMsbLsb(buffer);
        if (size < minBytes || size > maxBytes) {
            buffer.position(buffer.position() + size);
            return null;
        }
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return new String(bytes, UTF_8);
    }

    public final MqttFixedHeader getMqttFixedHeader() {
        return mqttFixedHeader;
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {

    }

    public void decodePlayLoad(ByteBuffer buffer) {

    }

    protected final int decodeMsbLsb(ByteBuffer buffer) {
        return decodeMsbLsb(buffer, 0, 65535);
    }

    protected final int decodeMsbLsb(ByteBuffer buffer, int min, int max) {
        short msbSize = BufferUtils.readUnsignedByte(buffer);
        short lsbSize = BufferUtils.readUnsignedByte(buffer);
        int result = msbSize << 8 | lsbSize;
        if (result < min || result > max) {
            result = -1;
        }
        return result;
    }

    protected final int decodeMessageId(ByteBuffer buffer) {
        final int messageId = decodeMsbLsb(buffer);
        if (messageId == 0) {
            throw new DecoderException("invalid messageId: " + messageId);
        }
        return messageId;
    }

    protected final boolean isValidPublishTopicName(String topicName) {
        // publish topic name must not contain any wildcard
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    public final byte[] decodeByteArray(ByteBuffer buffer) {
        final int decodedSize = decodeMsbLsb(buffer);
        byte[] bytes = new byte[decodedSize];
        buffer.get(bytes);
        return bytes;
    }
}
