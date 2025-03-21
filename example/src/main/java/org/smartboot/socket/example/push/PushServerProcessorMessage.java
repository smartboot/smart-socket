/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: PushServerProcessorMessage.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

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
    private Map<String, AioSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void process(AioSession session, String msg) {
        System.out.println("收到SendClient发送的消息:" + msg);
        byte[] bytes = msg.getBytes();
        sessionMap.values().forEach(onlineSession -> {
            if (session == onlineSession) {
                return;
            }
            WriteBuffer writeBuffer = onlineSession.writeBuffer();
            try {
                System.out.println("发送Push至ReceiverClient:" + onlineSession.getSessionID());
                writeBuffer.writeInt(bytes.length);
                writeBuffer.write(bytes);
                writeBuffer.flush();
            } catch (Exception e) {
                System.out.println("Push消息异常");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                System.out.println("与客户端:" + session.getSessionID() + " 建立连接");
                sessionMap.put(session.getSessionID(), session);
                break;
            case SESSION_CLOSED:
                System.out.println("断开客户端连接: " + session.getSessionID());
                sessionMap.remove(session.getSessionID());
                break;
            default:
        }
    }
}
