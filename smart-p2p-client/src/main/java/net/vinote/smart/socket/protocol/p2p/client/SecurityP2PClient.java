package net.vinote.smart.socket.protocol.p2p.client;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.HeartMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketMessageResp;
import net.vinote.smart.socket.security.RSA;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityP2PClient {
	private static Logger logger = LoggerFactory.getLogger(SecurityP2PClient.class);

	public static void main(String[] args) throws Exception {

		QuicklyConfig config = new QuicklyConfig(false);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setFilters(new SmartFilter[] { new FlowControlFilter() });
		config.setHost("127.0.0.1");
		config.setTimeout(1000);

		Properties properties = new Properties();
		properties.put(HeartMessageResp.class.getName(), "");
		properties.put(RemoteInterfaceMessageResp.class.getName(), "");
		properties.put(LoginAuthResp.class.getName(), "");
		properties.put(SecureSocketMessageResp.class.getName(), "");
		ServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		messageFactory.loadFromProperties(properties);
		config.setServiceMessageFactory(messageFactory);

		NioQuickClient client = new NioQuickClient(config);
		client.start();

		// 发送鉴权消息
		SecureSocketMessageReq publicKeyReq = new SecureSocketMessageReq();
		KeyPair keyPair = RSA.generateKeyPair();
		publicKeyReq.setRsaPublicKey(keyPair.getPublic().getEncoded());
		SecureSocketMessageResp resp = (SecureSocketMessageResp) processor.getSession().sendWithResponse(publicKeyReq);

		byte[] pubKey = RSA.decode(keyPair.getPrivate(), resp.getRsaPublicKey());
		PublicKey serverPubKey = RSA.generatePublicKey(pubKey);
		processor.getSession().setAttribute(StringUtils.SECRET_KEY, RSA.decode(serverPubKey, resp.getEncryptedKey()));

		long num = 100000;
		long start = System.currentTimeMillis();
		while (num-- > 0) {
			LoginAuthReq loginReq = new LoginAuthReq(processor.getSession().getAttribute(StringUtils.SECRET_KEY,
				byte[].class));
			loginReq.setUsername("zjw");
			loginReq.setPassword("aa");
			LoginAuthResp loginResp = (LoginAuthResp) processor.getSession().sendWithResponse(loginReq);
			logger.info(StringUtils.toHexString(loginResp.getData()));
		}
		logger.info("安全消息结束" + (System.currentTimeMillis() - start));

		num = 100000;
		start = System.currentTimeMillis();
		while (num-- > 0) {
			LoginAuthReq loginReq = new LoginAuthReq();
			loginReq.setUsername("zjw");
			loginReq.setPassword("aa");
			LoginAuthResp loginResp = (LoginAuthResp) processor.getSession().sendWithResponse(loginReq);

			logger.info(StringUtils.toHexString(loginResp.getData()));

		}
		logger.info("结束" + (System.currentTimeMillis() - start));
		client.shutdown();
	}
}
