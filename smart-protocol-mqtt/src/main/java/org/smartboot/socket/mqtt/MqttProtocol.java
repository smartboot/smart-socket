package org.smartboot.socket.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.message.MqttCodecUtil;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;

import static org.smartboot.socket.mqtt.MqttProtocol.DecoderState.FINISH;
import static org.smartboot.socket.mqtt.MqttProtocol.DecoderState.READ_FIXED_HEADER;
import static org.smartboot.socket.mqtt.MqttProtocol.DecoderState.READ_PAYLOAD;
import static org.smartboot.socket.mqtt.MqttProtocol.DecoderState.READ_VARIABLE_HEADER;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttProtocol implements Protocol<MqttMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MqttProtocol.class);
    private static final int DEFAULT_MAX_BYTES_IN_MESSAGE = 8092;
    private final int maxBytesInMessage;

    public MqttProtocol() {
        this(DEFAULT_MAX_BYTES_IN_MESSAGE);
    }

    public MqttProtocol(int maxBytesInMessage) {
        this.maxBytesInMessage = maxBytesInMessage;
    }


    @Override
    public MqttMessage decode(ByteBuffer buffer, AioSession<MqttMessage> session, boolean eof) {
        DecodeUnit unit;
        if (session.getAttachment() == null) {
            unit = new DecodeUnit();
            unit.state = READ_FIXED_HEADER;
            session.setAttachment(unit);
        } else {
            unit = (DecodeUnit) session.getAttachment();
        }
        switch (unit.state) {
            case READ_FIXED_HEADER:
                try {
                    if (buffer.remaining() < 2) {
                        break;
                    }
                    buffer.mark();
                    short b1 = BufferUtils.readUnsignedByte(buffer);

                    MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
                    boolean dupFlag = (b1 & 0x08) == 0x08;
                    int qosLevel = (b1 & 0x06) >> 1;
                    boolean retain = (b1 & 0x01) != 0;

                    int remainingLength = 0;
                    int multiplier = 1;
                    short digit;
                    int loops = 0;
                    do {
                        digit = BufferUtils.readUnsignedByte(buffer);
                        remainingLength += (digit & 127) * multiplier;
                        multiplier *= 128;
                        loops++;
                    } while (buffer.hasRemaining() && (digit & 128) != 0 && loops < 4);

                    //数据不足
                    if (!buffer.hasRemaining() && (digit & 128) != 0) {
                        buffer.reset();
                        break;
                    }
                    // MQTT protocol limits Remaining Length to 4 bytes
                    if (loops == 4 && (digit & 128) != 0) {
                        throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
                    }
                    buffer.mark();

                    MqttFixedHeader mqttFixedHeader =
                            new MqttFixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
                    MqttCodecUtil.resetUnusedFields(mqttFixedHeader);
                    switch (mqttFixedHeader.messageType()) {
                        case PUBREL:
                        case SUBSCRIBE:
                        case UNSUBSCRIBE:
                            if (mqttFixedHeader.qosLevel() != MqttQoS.AT_LEAST_ONCE) {
                                throw new DecoderException(mqttFixedHeader.messageType().name() + " message must have QoS 1");
                            }
                    }
                    unit.mqttMessage = MqttMessageFactory.newMessage(mqttFixedHeader);
                    unit.state = READ_VARIABLE_HEADER;

                } catch (Exception cause) {
                    unit.mqttMessage = MqttMessageFactory.newInvalidMessage(cause);
                    unit.state = FINISH;
                    break;
                }

            case READ_VARIABLE_HEADER:
                try {
                    if (unit.mqttMessage.getMqttFixedHeader().remainingLength() > maxBytesInMessage) {
                        throw new DecoderException("too large message: " + unit.mqttMessage.getMqttFixedHeader().remainingLength() + " bytes");
                    }
                    if (buffer.remaining() < unit.mqttMessage.getMqttFixedHeader().remainingLength()) {
                        break;
                    }
                    unit.mqttMessage.decodeVariableHeader(buffer);

                    unit.state = READ_PAYLOAD;

                    // fall through
                } catch (Exception cause) {
                    logger.error(unit.mqttMessage.toString());
                    unit.mqttMessage = MqttMessageFactory.newInvalidMessage(cause);
                    unit.state = FINISH;
                    break;
                }

            case READ_PAYLOAD:

                try {
                    unit.mqttMessage.decodePlayLoad(buffer);
                    unit.state = FINISH;
                    break;
                } catch (Exception cause) {
                    unit.mqttMessage = MqttMessageFactory.newInvalidMessage(cause);
                    unit.state = FINISH;
                    break;
                }

            default:
                // Shouldn't reach here.
                throw new Error();
        }
        if (unit.state == FINISH) {
            session.setAttachment(null);
            return unit.mqttMessage;
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer encode(MqttMessage msg, AioSession<MqttMessage> session) {

        return msg.encode();
    }

    enum DecoderState {
        READ_FIXED_HEADER,
        READ_VARIABLE_HEADER,
        READ_PAYLOAD,
        FINISH,
    }

    class DecodeUnit {
        DecoderState state;
        MqttMessage mqttMessage;
    }
}
