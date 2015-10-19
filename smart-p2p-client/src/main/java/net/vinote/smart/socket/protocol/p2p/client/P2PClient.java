package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.HeartMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class P2PClient {
	public static void main(String[] args) throws Exception {

		QuicklyConfig config = new QuicklyConfig(false);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setFilters(new SmartFilter[] { new FlowControlFilter() });
		config.setHost("127.0.0.1");
		config.setTimeout(1000);

		config.setServiceProcessorFactory(new ServiceProcessorManager());
		
		Properties properties = new Properties();
		properties.put(HeartMessageResp.class.getName(), "");
		properties.put(RemoteInterfaceMessageResp.class.getName(), "");
		properties.put(LoginAuthResp.class.getName(), "");
		ServiceMessageFactory messageFactory = new BaseMessageFactory(config);
		messageFactory.loadFromProperties(properties);
		
		
		NioQuickClient client = new NioQuickClient(config);
		client.start();

		long num = Long.MAX_VALUE;
		long start = System.currentTimeMillis();
		while (num-- > 0) {
			LoginAuthReq loginReq = new LoginAuthReq(
					processor.getSession().getAttribute(StringUtils.SECRET_KEY, byte[].class));
			loginReq.setUsername("zjw");
			loginReq.setPassword("aa");
			LoginAuthResp loginResp = (LoginAuthResp) processor.getSession().sendWithResponse(loginReq);
			/*
			 * RunLogger.getLogger().log(Level.FINE,
			 * StringUtils.toHexString(loginResp.getData()));
			 */
		}
		RunLogger.getLogger().log(Level.FINE, "安全消息结束" + (System.currentTimeMillis() - start));
		client.shutdown();
	}
}
