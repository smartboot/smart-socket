/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: PushClientProcessorMessage.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class PushClientProcessorMessage implements MessageProcessor<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushClientProcessorMessage.class);

    @Override
    public void process(AioSession session, String msg) {
        LOGGER.info("ReceiverClient:{} 收到Push消息:{}", session.getSessionID(), msg);
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }
}
