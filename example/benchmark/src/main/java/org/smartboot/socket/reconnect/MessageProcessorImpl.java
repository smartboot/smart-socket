/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReconnectMessageProcessor.java
 * Date: 2021-01-21
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.reconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/1/21
 */
public class MessageProcessorImpl implements MessageProcessor<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectServer.class);
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
