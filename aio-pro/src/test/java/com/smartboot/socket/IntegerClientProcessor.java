/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerClientProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class IntegerClientProcessor extends AbstractMessageProcessor<Integer> {

    @Override
    public void process0(AioSession session, Integer msg) {
        System.out.println("receive data from server：" + msg);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        System.out.println("other state:" + stateMachineEnum);
        if (stateMachineEnum == StateMachineEnum.INPUT_EXCEPTION) {
            throwable.printStackTrace();
        }
        if (stateMachineEnum == StateMachineEnum.OUTPUT_EXCEPTION) {
            throwable.printStackTrace();
        }
    }
}
