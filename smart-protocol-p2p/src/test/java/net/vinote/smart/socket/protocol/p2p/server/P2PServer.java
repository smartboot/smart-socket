package net.vinote.smart.socket.protocol.p2p.server;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.extension.timer.QuickMonitorTimer;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.filter.SecureFilter;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.HeartMessageReq;
import net.vinote.smart.socket.protocol.p2p.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.DetectMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.HeartMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.LoginAuthProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.RemoteServiceMessageProcessor;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class P2PServer {
	public static void main(String[] args) throws ClassNotFoundException {
		// 定义服务器接受的消息类型以及各类消息对应的处理器
		Properties properties = new Properties();
		properties.put(HeartMessageReq.class.getName(),
				HeartMessageProcessor.class.getName());
		properties.put(DetectMessageReq.class.getName(),
				DetectMessageProcessor.class.getName());
		properties.put(RemoteInterfaceMessageReq.class.getName(),
				RemoteServiceMessageProcessor.class.getName());
		properties.put(LoginAuthReq.class.getName(),
				LoginAuthProcessor.class.getName());
		BaseMessageFactory.getInstance().loadFromProperties(properties);

		QuicklyConfig config = new QuicklyConfig(true);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		config.setFilters(new SmartFilter[] { new FlowControlFilter(),
				new QuickMonitorTimer(), new SecureFilter() });
		ProtocolDataProcessor processor = new P2PServerDisruptorProcessor();
		// ProtocolDataProcessor processor = new P2PServerMessageProcessor();
		config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
		NioQuickServer server = new NioQuickServer(config);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
