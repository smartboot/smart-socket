package org.smartboot.socket.protocol.p2p.server;

import org.smartboot.socket.Filter;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.protocol.p2p.P2PProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.util.Properties;

public class P2PServer {
    public static void main(String[] args) throws ClassNotFoundException {
//        System.setProperty("javax.net.debug", "ssl");
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        Properties properties = new Properties();
//		properties.put(HeartMessageReq.class.getName(), HeartMessageProcessor.class.getName());
        properties.put(DetectMessageReq.class.getName(), DetectMessageHandler.class.getName());
//		properties.put(RemoteInterfaceMessageReq.class.getName(), RemoteServiceMessageProcessor.class.getName());
//		properties.put(LoginAuthReq.class.getName(), LoginAuthProcessor.class.getName());
//		properties.put(SecureSocketMessageReq.class.getName(), SecureSocketProcessor.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        messageFactory.loadFromProperties(properties);

//        AioSSLQuickServer<BaseMessage> server = new AioSSLQuickServer<BaseMessage>(9222, new P2PProtocol(messageFactory), new P2PServerMessageProcessor(messageFactory));
//        server.setClientAuth(ClientAuth.REQUIRE)
//                .setKeyStore("server.jks", "storepass")
//                .setTrust("trustedCerts.jks", "storepass")
//                .setKeyPassword("keypass")
//                .setThreadNum(16)
//                .setWriteQueueSize(16384)
//                .setFilters(new Filter[]{new QuickMonitorTimer<BaseMessage>()});


        AioQuickServer<BaseMessage> server = new AioQuickServer<BaseMessage>(8888, new P2PProtocol(messageFactory), new P2PServerMessageProcessor(messageFactory));
        server.setThreadNum(16)
                .setWriteQueueSize(16384)
//                .setDirectBuffer(true)
                .setFilters(new Filter[]{new QuickMonitorTimer<BaseMessage>()});
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
