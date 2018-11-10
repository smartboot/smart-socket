package org.smartboot.socket.protocol.p2p.server;

import org.smartboot.socket.extension.plugins.HeartPlugin;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.HeartMessageReq;
import org.smartboot.socket.protocol.p2p.message.HeartMessageRsp;
import org.smartboot.socket.protocol.p2p.message.MessageType;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.Properties;

public class P2PServer {
    public static void main(String[] args) throws ClassNotFoundException {
//        System.setProperty("javax.net.debug", "ssl");
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        Properties properties = new Properties();
//		properties.put(HeartMessageReq.class.getName(), HeartMessageProcessor.class.getName());
        properties.put(DetectMessageReq.class.getName(), DetectMessageHandler.class.getName());
        properties.put(HeartMessageReq.class.getName(), "");
        properties.put(HeartMessageRsp.class.getName(), "");
//		properties.put(RemoteInterfaceMessageReq.class.getName(), RemoteServiceMessageProcessor.class.getName());
//		properties.put(LoginAuthReq.class.getName(), LoginAuthProcessor.class.getName());
//		properties.put(SecureSocketMessageReq.class.getName(), SecureSocketProcessor.class.getName());
        final P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        messageFactory.loadFromProperties(properties);

//        AioSSLQuickServer<BaseMessage> server = new AioSSLQuickServer<BaseMessage>(9222, new P2PProtocol(messageFactory), new P2PServerMessageProcessor(messageFactory));
//        server.setClientAuth(ClientAuth.REQUIRE)
//                .setKeyStore("server.jks", "storepass")
//                .setTrust("trustedCerts.jks", "storepass")
//                .setKeyPassword("keypass")
//                .setThreadNum(16)
//                .setWriteQueueSize(16384)
//                .setFilters(new Filter[]{new QuickMonitorTimer<BaseMessage>()});

        P2PServerMessageProcessor processor = new P2PServerMessageProcessor(messageFactory);
        //注册服务监控插件
        processor.addPlugin(new MonitorPlugin());
        //注册心跳插件
//        processor.addPlugin(new HeartPlugin<BaseMessage>(-1) {
//            @Override
//            public void sendHeartRequest(AioSession<BaseMessage> session) throws IOException {
//                System.out.println("session:" + session + "发送心跳消息");
//                session.write(new HeartMessageReq());
//            }
//
//            @Override
//            public boolean isHeartMessage(AioSession<BaseMessage> session, BaseMessage msg) {
//                //收到非心跳消息
//                if (msg.getMessageType() != MessageType.HEART_MESSAGE_REQ
//                        && msg.getMessageType() != MessageType.HEART_MESSAGE_RSP) {
//                    return false;
//                }
//
//                if (msg.getMessageType() == MessageType.HEART_MESSAGE_REQ) {
//                    System.out.println("收到心跳请求消息:" + msg + ",发送心跳响应消息");
//                    try {
//                        //发送心跳响应消息
//                        session.write(new HeartMessageRsp());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                return true;
//            }
//        });
        AioQuickServer<BaseMessage> server = new AioQuickServer<BaseMessage>(8888, new P2PProtocol(messageFactory), processor);
        server
//                .setThreadNum(16)
                .setWriteQueueSize(16384)
//                .setDirectBuffer(true)
//                .setReadBufferSize(70)
        ;
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
