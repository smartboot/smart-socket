package org.smartboot.socket.mqtt.message;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttMessage {
    /**
     * 8-bit UTF (UCS Transformation Format)
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};
    protected MqttFixedHeader mqttFixedHeader = null;

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

    /**
     * 解码可变头部
     *
     * @param buffer
     */
    public void decodeVariableHeader(ByteBuffer buffer) {

    }

    public void decodePlayLoad(ByteBuffer buffer) {

    }

    public ByteBuffer encode() {
        return null;
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

    protected final byte getFixedHeaderByte1(MqttFixedHeader header) {
        int ret = 0;
        ret |= header.messageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.qosLevel().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return (byte) ret;
    }

    protected final byte[] decodeByteArray(ByteBuffer buffer) {
        final int decodedSize = decodeMsbLsb(buffer);
        byte[] bytes = new byte[decodedSize];
        buffer.get(bytes);
        return bytes;
    }

    protected final int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num /= 128;
            count++;
        } while (num > 0);
        return count;
    }

    protected final void writeVariableLengthInt(ByteBuffer buf, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            buf.put((byte) digit);
        } while (num > 0);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
