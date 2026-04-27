/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ServerProcessor.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.spring;

import io.github.smartboot.socket.MessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/1
 */
@Component("messageProcessor")
public class ServerProcessor implements MessageProcessor<String> {
    @Override
    public void process(AioSession session, String msg) {
        WriteBuffer outputStream = session.writeBuffer();
        try {
            byte[] bytes = msg.getBytes();
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
    }
}
