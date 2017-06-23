package net.vinote.smart.socket.protocol.p2p.server;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.extension.timer.QuickMonitorTimer;
import net.vinote.smart.socket.lang.QueueOverflowStrategy;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.processor.DetectMessageHandler;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class P2PServer {
	public static void main(String[] args) throws ClassNotFoundException {
		QuicklyConfig<BaseMessage> config = new QuicklyConfig<BaseMessage>(true);

		// 定义服务器接受的消息类型以及各类消息对应的处理器
		Properties properties = new Properties();
//		properties.put(HeartMessageReq.class.getName(), HeartMessageProcessor.class.getName());
		properties.put(DetectMessageReq.class.getName(), DetectMessageHandler.class.getName());
//		properties.put(RemoteInterfaceMessageReq.class.getName(), RemoteServiceMessageProcessor.class.getName());
//		properties.put(LoginAuthReq.class.getName(), LoginAuthProcessor.class.getName());
//		properties.put(SecureSocketMessageReq.class.getName(), SecureSocketProcessor.class.getName());
		P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		messageFactory.loadFromProperties(properties);
		config.setThreadNum(1);
		config.setProtocolFactory(new P2PProtocolFactory(messageFactory));
		config.setFilters(new SmartFilter[] { new QuickMonitorTimer<BaseMessage>() });
		// ProtocolDataProcessor processor = new P2PServerDisruptorProcessor();
		P2PServerMessageProcessor processor = new P2PServerMessageProcessor(messageFactory);
		config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
//		config.setQueueOverflowStrategy(QueueOverflowStrategy.DISCARD.name());
		NioQuickServer<BaseMessage> server = new NioQuickServer<BaseMessage>(config);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
