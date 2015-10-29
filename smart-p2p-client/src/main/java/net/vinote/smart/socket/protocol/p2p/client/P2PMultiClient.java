package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.HeartMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PMultiClient {
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			new Thread() {
				private Logger logger = LoggerFactory.getLogger(this.getClass());

				@Override
				public void run() {

					QuicklyConfig config = new QuicklyConfig(false);
					config.setProtocolFactory(new P2PProtocolFactory());
					P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
					config.setProcessor(processor);
					config.setFilters(new SmartFilter[] { new FlowControlFilter() });
					config.setHost("127.0.0.1");
					config.setTimeout(1000);

					Properties properties = new Properties();
					properties.put(HeartMessageResp.class.getName(), "");
					properties.put(RemoteInterfaceMessageResp.class.getName(), "");
					properties.put(LoginAuthResp.class.getName(), "");
					ServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
					try {
						messageFactory.loadFromProperties(properties);
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
					config.setServiceMessageFactory(messageFactory);

					NioQuickClient client = new NioQuickClient(config);
					client.start();

					long num = 0;
					long start = System.currentTimeMillis();
					while (num++ < Long.MAX_VALUE) {
						byte[] secretKey = processor.getSession().getAttribute(StringUtils.SECRET_KEY);
						LoginAuthReq loginReq = new LoginAuthReq(secretKey);
						loginReq.setUsername("zjw");
						loginReq.setPassword("aa");
						LoginAuthResp loginResp;
						try {
							loginResp = (LoginAuthResp) processor.getSession().sendWithResponse(loginReq);
							// processor.getSession().sendWithoutResponse(loginReq);
							// logger.info(StringUtils.toHexString(loginResp.getData()));
						} catch (Exception e) {
							System.out.println(num);
							e.printStackTrace();
							System.exit(0);
						}
					}
					logger.info("安全消息结束" + (System.currentTimeMillis() - start));
					client.shutdown();
				}

			}.start();
			Thread.sleep(500);
		}

	}
}
