package net.vinote.demo;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/25
 */
public class StringClientProcessor implements MessageProcessor<String> {
    private AioSession<String> session;

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        StringClientProcessor processor = new StringClientProcessor();
        AioQuickClient<String> client = new AioQuickClient<>("localhost", 8080, new StringProtocol(), processor);
        client.start();
        int i = 0;
        while (i++ < 10) {
            processor.getSession().write("Hello:" + i);
        }
        Thread.sleep(1000);
        client.shutdown();
    }

    @Override
    public void process(AioSession<String> session, String msg) {
        System.out.println("收到服务端消息：" + msg);
    }

    @Override
    public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        if (stateMachineEnum == StateMachineEnum.NEW_SESSION) {
            this.session = session;
        }
    }

    public AioSession<String> getSession() {
        return session;
    }
}
