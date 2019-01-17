package org.smartboot.socket.test;

import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerClient {
    public static void main(String[] args) throws Exception {
        AioQuickClient<Integer> aioQuickClient = new AioQuickClient<Integer>("localhost", 8888, new IntegerProtocol(), new IntegerClientProcessor());
        AioSession<Integer> session = aioQuickClient.start();
        session.writeBuffer().writeInt(1);
//        session.writeBuffer().flush();
//        Thread.sleep(1000);
        aioQuickClient.shutdownNow();
    }
}
