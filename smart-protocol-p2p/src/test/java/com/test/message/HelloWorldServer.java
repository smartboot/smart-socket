package com.test.message;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class HelloWorldServer {
	public static void main(String[] args) throws ClassNotFoundException {

		// 启动服务
		QuicklyConfig config = new QuicklyConfig(true);
		config.setProtocolFactory(new P2PProtocolFactory());//设置协议对象工厂
		config.setProcessor(new P2PServerMessageProcessor());//设置协议消息处理器

		// 注册消息以及对应的处理器
		Properties properties = new Properties();
		properties.put(HelloWorldReq.class.getName(), HelloWorldProcessor.class.getName());
		ServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		messageFactory.loadFromProperties(properties);
		config.setServiceMessageFactory(messageFactory);//设置业务消息处理工厂

		NioQuickServer server = new NioQuickServer(config);
		try {
			server.start();
		} catch (IOException e) {
			RunLogger.getLogger().log(e);
		}
	}
}
