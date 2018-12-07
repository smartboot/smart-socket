package net.vinote.demo;

import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerClient {
    public static void main(String[] args) throws Exception {
        IntegerClientProcessor processor = new IntegerClientProcessor();
        AioQuickClient<Integer> aioQuickClient = new AioQuickClient<Integer>("localhost", 8888, new IntegerProtocol(), processor);
        AioSession<Integer> session = aioQuickClient.start();
        session.write(1);
        Thread.sleep(1000);
        aioQuickClient.shutdown();
    }
}
