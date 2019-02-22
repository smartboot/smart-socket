package net.vinote.demo;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2019/2/22
 */
public class LocalAddressClient {
    public static void main(String[] args) throws Exception {
        MessageProcessor processor = new MessageProcessor() {
            @Override
            public void process(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        String remoteIp = "127.0.0.1";
        int remotePort = 8888;
        String localIp1 = "127.0.0.1";
        String localIp2 = "192.168.0.103";
        //IP、端口系统指定
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .start();
        //指定IP、端口
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .bindLocal(localIp2, 8080).start();
        //指定IP、端口
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .bindLocal(localIp1, 8080).start();
        //指定IP、端口随机
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .bindLocal(localIp2, 0).start();
        //指定IP、端口随机
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .bindLocal(localIp1, 0).start();
        //指定端口
        new AioQuickClient<Integer>(remoteIp, remotePort, null, processor)
                .bindLocal(null, 8081).start();
    }
}
