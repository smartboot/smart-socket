package net.vinote.smart.socket.protocol.p2p.server;

import net.vinote.smart.socket.protocol.p2p.QuickMonitorTimer;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.processor.DetectMessageHandler;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

import java.io.IOException;
import java.util.Properties;

public class P2PServer {
    public static void main(String[] args) throws ClassNotFoundException {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        Properties properties = new Properties();
//		properties.put(HeartMessageReq.class.getName(), HeartMessageProcessor.class.getName());
        properties.put(DetectMessageReq.class.getName(), DetectMessageHandler.class.getName());
//		properties.put(RemoteInterfaceMessageReq.class.getName(), RemoteServiceMessageProcessor.class.getName());
//		properties.put(LoginAuthReq.class.getName(), LoginAuthProcessor.class.getName());
//		properties.put(SecureSocketMessageReq.class.getName(), SecureSocketProcessor.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        messageFactory.loadFromProperties(properties);

        NioQuickServer<BaseMessage> server = new NioQuickServer<BaseMessage>().bind(8888).setThreadNum(4)
                .setFilters(new SmartFilter[]{new QuickMonitorTimer<BaseMessage>()})
                .setProtocolFactory(new P2PProtocolFactory(messageFactory))
                .setProcessor(new P2PServerMessageProcessor(messageFactory));
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
