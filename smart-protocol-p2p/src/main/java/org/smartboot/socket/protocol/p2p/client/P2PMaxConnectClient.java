package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;

/**
 * https://blog.csdn.net/wujunokay/article/details/46695065
 */
public class P2PMaxConnectClient {
    public static void main(String[] args) throws Exception {
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        int num = 0;
        try {
            while (true) {
                final AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>("118.25.26.239", 8888, null, new MessageProcessor<BaseMessage>() {
                    @Override
                    public void process(AioSession<BaseMessage> session, BaseMessage msg) {

                    }

                    @Override
                    public void stateEvent(AioSession<BaseMessage> session, StateMachineEnum stateEnum, Throwable throwable) {

                    }
                });
                client.setWriteQueueSize(0);
                client.setReadBufferSize(1);
                client.start(asynchronousChannelGroup);
                num++;
                Thread.sleep(0,50);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("已连接客户端：" + num);
        }
        System.out.println("finish");
        asynchronousChannelGroup.shutdown();
    }
}
