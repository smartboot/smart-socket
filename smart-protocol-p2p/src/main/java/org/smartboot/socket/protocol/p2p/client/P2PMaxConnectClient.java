package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

/**
 * https://blog.csdn.net/wujunokay/article/details/46695065
 */
public class P2PMaxConnectClient {
    public static void main(String[] args) throws Exception {
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(256, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        Properties properties = new Properties();
        properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        try {
            messageFactory.loadFromProperties(properties);
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        P2PProtocol protocol = new P2PProtocol(messageFactory);
        int num = 0;
        try {

            while (num<10000) {
                P2PClientMessageProcessor1 processor = new P2PClientMessageProcessor1();
                final AioQuickClient<BaseMessage> client = new AioQuickClient<BaseMessage>("localhost", 8888, protocol, processor);
                client.setWriteQueueSize(0);
                client.setReadBufferSize(60);
                client.start(asynchronousChannelGroup);
                DetectMessageReq request = new DetectMessageReq();
                request.setDetect("台州人在杭州:" + num);
                processor.getSession().write(request);
                num++;
                Thread.sleep(0, 50);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("已连接客户端：" + num);
        }
        System.out.println("finish");
//        asynchronousChannelGroup.shutdown();
    }
}

class P2PClientMessageProcessor1 implements MessageProcessor<BaseMessage> {

    private AioSession<BaseMessage> session;

    @Override
    public void process(AioSession<BaseMessage> ioSession, BaseMessage msg) {
//        System.out.println(msg);
        DetectMessageReq request = new DetectMessageReq();
        request.setDetect("台州人在杭州:");
        try {
            ioSession.write(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<BaseMessage> ioSession, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                session = ioSession;
                break;
        }

    }

    public AioSession<BaseMessage> getSession() {
        return session;
    }

}