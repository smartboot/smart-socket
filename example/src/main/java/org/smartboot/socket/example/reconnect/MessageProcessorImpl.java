/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MessageProcessorImpl.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.reconnect;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/1/21
 */
public class MessageProcessorImpl implements MessageProcessor<String> {
    @Override
    public void process(AioSession session, String msg) {

    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
//                LOGGER.info("客户端:{} 建立连接", session.getSessionID());
            case SESSION_CLOSED:
//                LOGGER.info("客户端:{} 断开连接", session.getSessionID());
                break;
        }
    }
}
