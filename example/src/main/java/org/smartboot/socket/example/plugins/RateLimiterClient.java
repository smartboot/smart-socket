package org.smartboot.socket.example.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author huqiang
 * @since 2021/12/19 16:57
 */
public class RateLimiterClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        AbstractMessageProcessor<String> messageProcessor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                System.out.println(" 请求数据：" + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        AioQuickClient aioQuickClient = new AioQuickClient("localhost", 8888, new StringProtocol(), messageProcessor);
        aioQuickClient.setWriteBuffer(1024 * 1024, 512);
        AioSession aioSession = aioQuickClient.start();
        while (true) {
            WriteBuffer writeBuffer = aioSession.writeBuffer();
            byte[] bytes = new byte[10 * 1024];
            writeBuffer.writeInt(bytes.length);
            writeBuffer.write(bytes);
            writeBuffer.flush();
            Thread.sleep(500);
        }
    }
}
