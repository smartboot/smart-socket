package org.smartboot.socket.example.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.IdleStatePlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author huqiang
 * @since 2021/12/20 11:18
 */
public class IdleStatePluginDemo {

    public static void main(String[] args) throws IOException, InterruptedException {

        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                System.out.println("服务端接收数据：" + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        AioQuickServer server = new AioQuickServer("localhost", 8888, new StringProtocol(), processor);
        processor.addPlugin(new IdleStatePlugin<>(5000));
        server.start();

        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                System.out.println("客户端接收数据：" + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        });

        AioSession start = client.start();
        int i = 0;
        while (true) {
            i++;
            WriteBuffer writeBuffer = start.writeBuffer();
            if (i % 5 == 0) {
                byte[] content = "idle message".getBytes();
                writeBuffer.writeInt(content.length);
                writeBuffer.write(content);
                writeBuffer.flush();
                Thread.sleep(2000L);
            } else {
                byte[] content = ("content message -" + i).getBytes();
                writeBuffer.writeInt(content.length);
                writeBuffer.write(content);
                writeBuffer.flush();
                Thread.sleep(1000L);
            }
        }


    }
}
