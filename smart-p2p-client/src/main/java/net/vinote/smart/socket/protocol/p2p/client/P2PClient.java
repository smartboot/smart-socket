package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.HeartMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class P2PClient {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.put(HeartMessageResp.class.getName(), "");
		properties.put(RemoteInterfaceMessageResp.class.getName(), "");
		properties.put(LoginAuthResp.class.getName(), "");
		BaseMessageFactory.getInstance().loadFromProperties(properties);
		QuicklyConfig config = new QuicklyConfig(false);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setFilters(new SmartFilter[] { new FlowControlFilter() });
		config.setHost("127.0.0.1");
		config.setTimeout(1000);
		NioQuickClient client = new NioQuickClient(config);
		client.start();

	}
}
