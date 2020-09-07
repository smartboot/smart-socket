/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: PushServerProcessorMessage.java
 * Date: 2020-04-25
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class PushServerProcessorMessage implements MessageProcessor<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushServerProcessorMessage.class);
    private Map<String, AioSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void process(AioSession session, String msg) {
        LOGGER.info("收到SendClient发送的消息:{}", msg);
        byte[] bytes = msg.getBytes();
        sessionMap.values().forEach(onlineSession -> {
            if (session == onlineSession) {
                return;
            }
            WriteBuffer writeBuffer = onlineSession.writeBuffer();
            try {
                LOGGER.info("发送Push至ReceiverClient:{}", onlineSession.getSessionID());
                writeBuffer.writeInt(bytes.length);
                writeBuffer.write(bytes);
                writeBuffer.flush();
            } catch (Exception e) {
                LOGGER.error("Push消息异常", e);
            }
        });
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                LOGGER.info("与客户端:{} 建立连接", session.getSessionID());
                sessionMap.put(session.getSessionID(), session);
                break;
            case SESSION_CLOSED:
                LOGGER.info("断开客户端连接: {}", session.getSessionID());
                sessionMap.remove(session.getSessionID());
                break;
            default:
        }
    }
}
