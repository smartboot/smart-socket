package org.smartboot.socket.mqtt.message;

import org.smartboot.socket.mqtt.MqttFixedHeader;
import org.smartboot.socket.mqtt.MqttIdentifierRejectedException;
import org.smartboot.socket.mqtt.MqttVersion;
import org.smartboot.socket.util.BufferUtils;
import org.smartboot.socket.util.DecoderException;

import java.nio.ByteBuffer;

import static org.smartboot.socket.mqtt.message.MqttCodecUtil.isValidClientId;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class MqttConnectMessage extends MqttMessage {
    private MqttConnectVariableHeader mqttConnectVariableHeader;
    private MqttConnectPayload mqttConnectPayload;

    public MqttConnectMessage(MqttFixedHeader mqttFixedHeader) {
        super(mqttFixedHeader);
    }

    @Override
    public void decodeVariableHeader(ByteBuffer buffer) {
        final String protoString = decodeString(buffer);

        final byte protocolLevel = buffer.get();

        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(protoString, protocolLevel);

        final int b1 = BufferUtils.readUnsignedByte(buffer);

        final int keepAlive = decodeMsbLsb(buffer);

        final boolean hasUserName = (b1 & 0x80) == 0x80;
        final boolean hasPassword = (b1 & 0x40) == 0x40;
        final boolean willRetain = (b1 & 0x20) == 0x20;
        final int willQos = (b1 & 0x18) >> 3;
        final boolean willFlag = (b1 & 0x04) == 0x04;
        final boolean cleanSession = (b1 & 0x02) == 0x02;
        if (mqttVersion == MqttVersion.MQTT_3_1_1) {
            final boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
            if (!zeroReservedFlag) {
                // MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
                // set to zero and disconnect the Client if it is not zero.
                // See http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
                throw new DecoderException("non-zero reserved flag");
            }
        }

        mqttConnectVariableHeader = new MqttConnectVariableHeader(
                mqttVersion.protocolName(),
                mqttVersion.protocolLevel(),
                hasUserName,
                hasPassword,
                willRetain,
                willQos,
                willFlag,
                cleanSession,
                keepAlive);
    }

    @Override
    public void decodePlayLoad(ByteBuffer buffer) {
        final String decodedClientId = decodeString(buffer);
        final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(mqttConnectVariableHeader.name(),
                (byte) mqttConnectVariableHeader.version());
        if (!isValidClientId(mqttVersion, decodedClientId)) {
            throw new MqttIdentifierRejectedException("invalid clientIdentifier: " + decodedClientId);
        }

        String decodedWillTopic = null;
        byte[] decodedWillMessage = null;
        if (mqttConnectVariableHeader.isWillFlag()) {
            decodedWillTopic = decodeString(buffer, 0, 32767);
            decodedWillMessage = decodeByteArray(buffer);
        }
        String decodedUserName = null;
        byte[] decodedPassword = null;
        if (mqttConnectVariableHeader.hasUserName()) {
            decodedUserName = decodeString(buffer);
        }
        if (mqttConnectVariableHeader.hasPassword()) {
            decodedPassword = decodeByteArray(buffer);
        }

        mqttConnectPayload =
                new MqttConnectPayload(
                        decodedClientId,
                        decodedWillTopic,
                        decodedWillMessage,
                        decodedUserName,
                        decodedPassword);
    }

    public final class MqttConnectPayload {

        private final String clientIdentifier;
        private final String willTopic;
        private final byte[] willMessage;
        private final String userName;
        private final byte[] password;

        /**
         * @deprecated use {@link MqttConnectPayload#MqttConnectPayload(String, String, byte[], String, byte[])} instead
         */
        @Deprecated
        public MqttConnectPayload(
                String clientIdentifier,
                String willTopic,
                String willMessage,
                String userName,
                String password) {
            this(
                    clientIdentifier,
                    willTopic,
                    willMessage.getBytes(UTF_8),
                    userName,
                    password.getBytes(UTF_8));
        }

        public MqttConnectPayload(
                String clientIdentifier,
                String willTopic,
                byte[] willMessage,
                String userName,
                byte[] password) {
            this.clientIdentifier = clientIdentifier;
            this.willTopic = willTopic;
            this.willMessage = willMessage;
            this.userName = userName;
            this.password = password;
        }

        public String clientIdentifier() {
            return clientIdentifier;
        }

        public String willTopic() {
            return willTopic;
        }

        /**
         * @deprecated use {@link MqttConnectPayload#willMessageInBytes()} instead
         */
        @Deprecated
        public String willMessage() {
            return new String(willMessage, UTF_8);
        }

        public byte[] willMessageInBytes() {
            return willMessage;
        }

        public String userName() {
            return userName;
        }

        /**
         * @deprecated use {@link MqttConnectPayload#passwordInBytes()} instead
         */
        @Deprecated
        public String password() {
            return new String(password, UTF_8);
        }

        public byte[] passwordInBytes() {
            return password;
        }


    }
}
