package com.test.message;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.io.nio.NioQuickServer;

public class HelloWorldServer {
	public static void main(String[] args) throws ClassNotFoundException {
		// 注册消息以及对应的处理器
		Properties properties = new Properties();
		properties.put(HelloWorldReq.class.getName(), HelloWorldHandler.class.getName());
		P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		messageFactory.loadFromProperties(properties);
		// 启动服务
		QuicklyConfig<BaseMessage> config = new QuicklyConfig<BaseMessage>(true);
		config.setProtocolFactory(new P2PProtocolFactory(messageFactory));// 设置协议对象工厂
		config.setProcessor(new P2PServerMessageProcessor(messageFactory));

		NioQuickServer<BaseMessage> server = new NioQuickServer<BaseMessage>(config);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
