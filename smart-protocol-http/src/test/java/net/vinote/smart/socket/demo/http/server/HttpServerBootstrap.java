package net.vinote.smart.socket.demo.http.server;

import net.vinote.smart.socket.demo.http.application.AbstractServletContext;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.HttpProtocolFactory;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

public class HttpServerBootstrap {
	public static void start(AbstractServletContext app) {

		QuicklyConfig config = new QuicklyConfig(true);
		config.setPort(8081);
		config.setProcessor(new HttpProtocolMessageProcessor(app));
		config.setProtocolFactory(new HttpProtocolFactory());
		// 启动服务系统
		NioQuickServer server = null;
		try {
			server = new NioQuickServer(config);
			server.start();
		} catch (Exception e1) {
			if (server != null) {
				server.shutdown();
			}
			return;
		}
		System.out.println("启动系统服务...");

	}

	public static void main(String[] args) {
		start(null);
	}
}
