package org.smartboot.socket.heart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class HeartClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartClient.class);


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        AbstractMessageProcessor<String> client_1_processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                LOGGER.info("client_1 收到服务端消息:" + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                LOGGER.info("stateMachineEnum：{}", stateMachineEnum);
            }
        };
        AioQuickClient<String> client_1 = new AioQuickClient<>("localhost", 8888, new StringProtocol(), client_1_processor);
        client_1.start();

        AbstractMessageProcessor<String> client_2_processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                LOGGER.info("client_2 收到服务端消息:" + msg);
                try {
                    if ("heart_req".equals(msg)) {
                        WriteBuffer writeBuffer = session.writeBuffer();
                        byte[] heartBytes = "heart_rsp".getBytes();
                        writeBuffer.writeInt(heartBytes.length);
                        writeBuffer.write(heartBytes);
                        LOGGER.info("client_2 发送心跳响应消息");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                LOGGER.info("stateMachineEnum：{}", stateMachineEnum);
            }
        };
        AioQuickClient<String> client_2 = new AioQuickClient<>("localhost", 8888, new StringProtocol(), client_2_processor);
        client_2.start();
    }
}
