package org.smartboot.socket.example.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.HeartPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author huqiang
 * @since 2021/12/20 11:18
 */
public class HeartPluginDemo {

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
        processor.addPlugin(new HeartPlugin<String>(5, 7, TimeUnit.SECONDS) {
            @Override
            public void sendHeartRequest(AioSession session) throws IOException {
                WriteBuffer writeBuffer = session.writeBuffer();
                byte[] content = "heart message".getBytes();
                writeBuffer.writeInt(content.length);
                writeBuffer.write(content);
            }

            @Override
            public boolean isHeartMessage(AioSession session, String msg) {
                return "heart message".equals(msg);
            }
        });
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
                byte[] content = "heart message".getBytes();
                writeBuffer.writeInt(content.length);
                writeBuffer.write(content);
                Thread.sleep(2000L);
            } else if (i % 31 == 0) {
                // 连接超时关闭 , 设置的7秒超时关闭连接，这里休眠10s
                Thread.sleep(10000L);
                try {
                    byte[] content = ("content message -" + i).getBytes();
                    writeBuffer.writeInt(content.length);
                    writeBuffer.write(content);
                } catch (IOException e) {
                    System.out.println("心跳检测超时----- ");
                    assert "writeBuffer has closed".equals(e.getMessage());
                }
                break;
            } else {
                byte[] content = ("content message -" + i).getBytes();
                writeBuffer.writeInt(content.length);
                writeBuffer.write(content);
                Thread.sleep(1000L);
            }
        }


    }
}
