package com.test.message;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class HelloWorldServer {
	public static void main(String[] args) throws ClassNotFoundException {

		// 启动服务
		QuicklyConfig config = new QuicklyConfig(true);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		ProtocolDataProcessor processor = new P2PServerMessageProcessor();
		config.setProcessor(processor);

		config.setServiceProcessorFactory(new ServiceProcessorManager());
		
		// 注册消息以及对应的处理器
		Properties properties = new Properties();
		properties.put(HelloWorldReq.class.getName(), HelloWorldProcessor.class.getName());
		ServiceMessageFactory messageFactory = new BaseMessageFactory(config);
		messageFactory.loadFromProperties(properties);

		NioQuickServer server = new NioQuickServer(config);
		try {
			server.start();
		} catch (IOException e) {
			RunLogger.getLogger().log(e);
		}
	}
}
