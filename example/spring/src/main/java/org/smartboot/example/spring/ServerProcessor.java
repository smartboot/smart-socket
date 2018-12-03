package org.smartboot.example.spring;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.BufferOutputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/1
 */
@Component("messageProcessor")
public class ServerProcessor implements MessageProcessor<String> {
    @Override
    public void process(AioSession<String> session, String msg) {
        BufferOutputStream outputStream = session.getOutputStream();
        byte[] bytes = msg.getBytes();
        outputStream.writeInt(bytes.length);
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
    }
}
