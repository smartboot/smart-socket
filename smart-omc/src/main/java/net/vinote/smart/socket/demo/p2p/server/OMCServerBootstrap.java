package net.vinote.smart.socket.demo.p2p.server;

import java.io.IOException;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.server.P2PServerMessageProcessor;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class OMCServerBootstrap {
	public static void main(String[] args) {
		QuicklyConfig config = new QuicklyConfig();
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		ProtocolDataProcessor processor = new P2PServerMessageProcessor();
		config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
		NioQuickServer server = new NioQuickServer(config);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
