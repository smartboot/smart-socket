/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerServerProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.test;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class IntegerServerProcessor implements MessageProcessor<Integer> {
    @Override
    public void process(AioSession<Integer> session, Integer msg) {
        Integer respMsg = msg + 1;
        System.out.println("receive data from client: " + msg + " ,rsp:" + (respMsg));
        try {
            session.writeBuffer().writeInt(respMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<Integer> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }
}
