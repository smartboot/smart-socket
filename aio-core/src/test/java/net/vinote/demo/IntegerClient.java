package net.vinote.demo;

import org.smartboot.socket.transport.AioQuickClient;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerClient {
    public static void main(String[] args) throws Exception {
        IntegerClientProcessor processor = new IntegerClientProcessor();
        AioQuickClient<Integer> aioQuickClient = new AioQuickClient<Integer>()
                .connect("localhost", 8888)
                .setProtocol(new IntegerProtocol())
                .setProcessor(processor);
        aioQuickClient.start();
        processor.getSession().write(1);
        Thread.sleep(1000);
        aioQuickClient.shutdown();

    }
}
