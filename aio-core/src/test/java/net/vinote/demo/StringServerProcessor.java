package net.vinote.demo;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/25
 */
public class StringServerProcessor implements MessageProcessor<String> {
    public static void main(String[] args) throws IOException {
        AioQuickServer<String> server = new AioQuickServer<>(8080, new StringProtocol(), new StringServerProcessor());
        server.start();
    }

    @Override
    public void process(AioSession<String> session, String msg) {
        System.out.println("收到客户端消息:" + msg);
        try {
            session.write("服务端收到了你的消息:" + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }
}
