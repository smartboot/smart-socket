package org.smartboot.socket.heart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class HeartClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartClient.class);


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession<String> session, String msg) {
                LOGGER.info("收到服务端消息:" + msg);
            }

            @Override
            public void stateEvent0(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                LOGGER.info("stateMachineEnum：{}", stateMachineEnum);
            }
        };
        AioQuickClient<String> client = new AioQuickClient<>("localhost", 8888, new StringProtocol(), processor);
        client.start();
    }
}
