package org.smartboot.socket.mqtt.processor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.mqtt.MqttContext;
import org.smartboot.socket.mqtt.MqttMessageBuilders;
import org.smartboot.socket.mqtt.MqttSession;
import org.smartboot.socket.mqtt.enums.MqttConnectReturnCode;
import org.smartboot.socket.mqtt.enums.MqttMessageType;
import org.smartboot.socket.mqtt.enums.MqttQoS;
import org.smartboot.socket.mqtt.enums.MqttVersion;
import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttConnAckVariableHeader;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.message.MqttConnectPayload;
import org.smartboot.socket.mqtt.message.MqttFixedHeader;
import org.smartboot.socket.mqtt.processor.MqttProcessor;

import java.util.UUID;

import static org.smartboot.socket.mqtt.enums.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static org.smartboot.socket.mqtt.enums.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/25
 */
public class ConnectProcessor implements MqttProcessor<MqttConnectMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectProcessor.class);

    private boolean allowZeroByteClientId=false;
    @Override
    public void process(MqttContext context, MqttSession session, MqttConnectMessage mqttConnectMessage) {
        LOGGER.info("receive connect message:{}", mqttConnectMessage);
        MqttConnAckMessage mqttConnAckMessage = MqttMessageBuilders.connAck()
                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                .sessionPresent(true).build();
        session.write(mqttConnAckMessage);
        LOGGER.info("response connect message:{}", mqttConnAckMessage);


        MqttConnectPayload payload = mqttConnectMessage.getayload();
        String clientId = payload.clientIdentifier();
        LOGGER.info("Processing CONNECT message. CId={}, username={}", clientId, payload.userName());

        if (mqttConnectMessage.getVariableHeader().version() != MqttVersion.MQTT_3_1.protocolLevel()
                && mqttConnectMessage.getVariableHeader().version() != MqttVersion.MQTT_3_1_1.protocolLevel()) {
            MqttConnAckMessage badProto = connAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION,false);

            LOGGER.error("MQTT protocol version is not valid. CId={}", clientId);
            session.write(badProto);
            session.close();
            return;
        }

        final boolean cleanSession = mqttConnectMessage.getVariableHeader().isCleanSession();
        if (clientId == null || clientId.length() == 0) {
            if (!cleanSession || !this.allowZeroByteClientId) {
                MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED,false);

                session.write(badId);
                session.close();
                LOGGER.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
                return;
            }

            // Generating client id.
            clientId = UUID.randomUUID().toString().replace("-", "");
            LOGGER.info("Client has connected with a server generated identifier. CId={}, username={}", clientId,
                    payload.userName());
        }

        if (!login(channel, msg, clientId)) {
            session.close();
            return;
        }

        session.init(clientId,cleanSession);
        MqttSession existing = context.addSession(session);
        if (existing != null) {
            LOGGER.info("Client ID is being used in an existing connection, force to be closed. CId={}", clientId);
            existing.abort();
            //return;
            this.connectionDescriptors.removeConnection(existing);
            this.connectionDescriptors.addConnection(descriptor);
        }

        initializeKeepAliveTimeout(channel, msg, clientId);
        storeWillMessage(msg, clientId);
        if (!sendAck(descriptor, msg, clientId)) {
            channel.close();
            return;
        }

        m_interceptor.notifyClientConnected(msg);

        if (!descriptor.assignState(SENDACK, SESSION_CREATED)) {
            channel.close();
            return;
        }
        final ClientSession clientSession = this.sessionsRepository.createOrLoadClientSession(clientId, cleanSession);

        if (!republish(descriptor, msg, clientSession)) {
            channel.close();
            return;
        }
        final boolean success = descriptor.assignState(MESSAGES_REPUBLISHED, ESTABLISHED);
        if (!success) {
            session.close();
        }

        LOGGER.info("CONNECT message processed CId={}, username={}", clientId, payload.userName());
    }

    private boolean login(Channel channel, MqttConnectMessage msg, final String clientId) {
        // handle user authentication
        if (msg.variableHeader().hasUserName()) {
            byte[] pwd = null;
            if (msg.variableHeader().hasPassword()) {
                pwd = msg.payload().password().getBytes();
            } else if (!this.allowAnonymous) {
                LOG.error("Client didn't supply any password and MQTT anonymous mode is disabled CId={}", clientId);
                failedCredentials(channel);
                return false;
            }
            if (!m_authenticator.checkValid(clientId, msg.payload().userName(), pwd)) {
                LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}, password={}",
                        clientId, msg.payload().userName(), pwd);
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, msg.payload().userName());
        } else if (!this.allowAnonymous) {
            LOG.error("Client didn't supply any credentials and MQTT anonymous mode is disabled. CId={}", clientId);
            failedCredentials(channel);
            return false;
        }
        return true;
    }
    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }
}
