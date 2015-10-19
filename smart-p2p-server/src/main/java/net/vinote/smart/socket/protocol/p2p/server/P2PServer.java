package net.vinote.smart.socket.protocol.p2p.server;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.extension.timer.QuickMonitorTimer;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.filter.SecureFilter;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.HeartMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.DetectMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.HeartMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.LoginAuthProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.RemoteServiceMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.SecureSocketProcessor;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.factory.ServiceProcessorFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class P2PServer {
	public static void main(String[] args) throws ClassNotFoundException {
		QuicklyConfig config = new QuicklyConfig(true);
		
		ServiceProcessorFactory processorFactory=new ServiceProcessorManager();
		config.setServiceProcessorFactory(processorFactory);
		
		// 定义服务器接受的消息类型以及各类消息对应的处理器
		Properties properties = new Properties();
		properties.put(HeartMessageReq.class.getName(), HeartMessageProcessor.class.getName());
		properties.put(DetectMessageReq.class.getName(), DetectMessageProcessor.class.getName());
		properties.put(RemoteInterfaceMessageReq.class.getName(), RemoteServiceMessageProcessor.class.getName());
		properties.put(LoginAuthReq.class.getName(), LoginAuthProcessor.class.getName());
		properties.put(SecureSocketMessageReq.class.getName(), SecureSocketProcessor.class.getName());
		ServiceMessageFactory messageFactory = new BaseMessageFactory(config);
		messageFactory.loadFromProperties(properties);
		
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		config.setFilters(new SmartFilter[] { new FlowControlFilter(), new QuickMonitorTimer(), new SecureFilter() });
		// ProtocolDataProcessor processor = new P2PServerDisruptorProcessor();
		ProtocolDataProcessor processor = new P2PServerMessageProcessor();
		config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
		NioQuickServer server = new NioQuickServer(config);
		try {
			server.start();
		} catch (IOException e) {
			RunLogger.getLogger().log(e);
		}
	}
}
