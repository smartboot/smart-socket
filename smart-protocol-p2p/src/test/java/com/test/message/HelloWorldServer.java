package com.test.message;

import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

import java.io.IOException;
import java.util.Properties;

public class HelloWorldServer {
    public static void main(String[] args) throws ClassNotFoundException {
        // 注册消息以及对应的处理器
        Properties properties = new Properties();
        properties.put(HelloWorldReq.class.getName(), HelloWorldHandler.class.getName());
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        messageFactory.loadFromProperties(properties);
        // 启动服务

        NioQuickServer<BaseMessage> server = new NioQuickServer<BaseMessage>()
//                .setProcessor(new P2PServerMessageProcessor(messageFactory))
                .setProtocolFactory(new P2PProtocolFactory(messageFactory));
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
