package net.vinote.smart.socket.demo.p2p.server;

import java.io.IOException;
import java.util.Properties;

import net.vinote.smart.socket.demo.p2p.server.processor.DetectMessageProcessor;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class OMCServerBootstrap {
	public static void main(String[] args) throws ClassNotFoundException {
		QuicklyConfig config = new QuicklyConfig(true);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		ProtocolDataProcessor processor = new P2PServerMessageProcessor();
		config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
		NioQuickServer server = new NioQuickServer(config);
		Properties msgProcessorPro = new Properties();
		msgProcessorPro.put(DetectMessageReq.class.getName(),
				DetectMessageProcessor.class.getName());
		BaseMessageFactory.getInstance().loadFromProperties(msgProcessorPro);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
