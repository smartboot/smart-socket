/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.smartboot.socket.mqtt;

import org.smartboot.socket.mqtt.message.MqttConnAckMessage;
import org.smartboot.socket.mqtt.message.MqttConnectMessage;
import org.smartboot.socket.mqtt.message.MqttDisconnectMessage;
import org.smartboot.socket.mqtt.message.MqttInvalidMessage;
import org.smartboot.socket.mqtt.message.MqttMessage;
import org.smartboot.socket.mqtt.message.MqttPingReqMessage;
import org.smartboot.socket.mqtt.message.MqttPingRespMessage;
import org.smartboot.socket.mqtt.message.MqttPubAckMessage;
import org.smartboot.socket.mqtt.message.MqttPubCompMessage;
import org.smartboot.socket.mqtt.message.MqttPublishMessage;
import org.smartboot.socket.mqtt.message.MqttPubrecMessage;
import org.smartboot.socket.mqtt.message.MqttPubrelMessage;
import org.smartboot.socket.mqtt.message.MqttSubAckMessage;
import org.smartboot.socket.mqtt.message.MqttSubscribeMessage;
import org.smartboot.socket.mqtt.message.MqttUnsubAckMessage;
import org.smartboot.socket.mqtt.message.MqttUnsubscribeMessage;

/**
 * Utility class with factory methods to create different types of MQTT messages.
 */
public final class MqttMessageFactory {

    private MqttMessageFactory() {
    }

    public static MqttMessage newMessage(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.messageType()) {
            case CONNECT:
                return new MqttConnectMessage(mqttFixedHeader);

            case CONNACK:
                return new MqttConnAckMessage(mqttFixedHeader);

            case SUBSCRIBE:
                return new MqttSubscribeMessage(mqttFixedHeader);

            case SUBACK:
                return new MqttSubAckMessage(mqttFixedHeader);

            case UNSUBACK:
                return new MqttUnsubAckMessage(mqttFixedHeader);

            case UNSUBSCRIBE:
                return new MqttUnsubscribeMessage(mqttFixedHeader);

            case PUBLISH:
                return new MqttPublishMessage(mqttFixedHeader);

            case PUBACK:
                return new MqttPubAckMessage(mqttFixedHeader);
            case PUBREC:
                return new MqttPubrecMessage(mqttFixedHeader);
            case PUBREL:
                return new MqttPubrelMessage(mqttFixedHeader);
            case PUBCOMP:
                return new MqttPubCompMessage(mqttFixedHeader);

            case PINGREQ:
                return new MqttPingReqMessage(mqttFixedHeader);
            case PINGRESP:
                return new MqttPingRespMessage(mqttFixedHeader);
            case DISCONNECT:
                return new MqttDisconnectMessage(mqttFixedHeader);

            default:
                throw new IllegalArgumentException("unknown message type: " + mqttFixedHeader.messageType());
        }
    }

    public static MqttMessage newInvalidMessage(Throwable cause) {
        cause.printStackTrace();
        return new MqttInvalidMessage(null);
    }
}
