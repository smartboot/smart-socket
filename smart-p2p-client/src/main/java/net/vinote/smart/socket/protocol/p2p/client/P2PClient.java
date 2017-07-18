package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.extension.timer.QuickMonitorTimer;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class P2PClient {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.put(DetectMessageResp.class.getName(), DetectRespMessageHandler.class.getName());
		P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		try {
			messageFactory.loadFromProperties(properties);
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		QuicklyConfig<BaseMessage> config = new QuicklyConfig<BaseMessage>(false);
		config.setProtocolFactory(new P2PProtocolFactory(messageFactory));
		final P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
		config.setProcessor(processor);
		config.setFilters(new SmartFilter[] { new QuickMonitorTimer<BaseMessage>() });
		config.setHost("127.0.0.1");
		config.setTimeout(1000);

		final NioQuickClient<BaseMessage> client = new NioQuickClient<BaseMessage>(config);
		client.start();

		for (int i = 0; i < 1; i++) {
			new Thread() {
				private Logger logger = LogManager.getLogger(this.getClass());

				@Override
				public void run() {

					long num = 0;
					long start = System.currentTimeMillis();
					while (num++ < Long.MAX_VALUE) {
						DetectMessageReq request = new DetectMessageReq();
						try {
//							DetectMessageResp loginResp = (DetectMessageResp) processor.getSession()
//								.sendWithResponse(request);
							 processor.getSession().sendWithoutResponse(request);
//							 Thread.sleep(1);
							// logger.info(loginResp);
						} catch (Exception e) {
							System.out.println(num);
							e.printStackTrace();
//							System.exit(0);
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
