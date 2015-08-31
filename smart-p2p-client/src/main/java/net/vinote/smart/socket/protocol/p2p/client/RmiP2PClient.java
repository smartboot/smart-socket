package net.vinote.smart.socket.protocol.p2p.client;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketMessageResp;
import net.vinote.smart.socket.security.RSA;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;
import net.vinote.smartweb.service.facade.UserInfoService;
import net.vinote.smartweb.service.facade.vo.UserInfoVO;
import net.vinote.smartweb.service.result.SmartResult;

public class RmiP2PClient {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.put(SecureSocketMessageResp.class.getName(), "");
		properties.put(LoginAuthResp.class.getName(), "");
		properties.put(RemoteInterfaceMessageResp.class.getName(), "");
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

		// 发送鉴权消息
		SecureSocketMessageReq publicKeyReq = new SecureSocketMessageReq();
		KeyPair keyPair = RSA.generateKeyPair();
		publicKeyReq.setRsaPublicKey(keyPair.getPublic().getEncoded());
		SecureSocketMessageResp resp = (SecureSocketMessageResp) processor
				.getSession().sendWithResponse(publicKeyReq);

		byte[] pubKey = RSA
				.decode(keyPair.getPrivate(), resp.getRsaPublicKey());
		PublicKey serverPubKey = RSA.generatePublicKey(pubKey);
		processor.getSession().setAttribute(StringUtils.SECRET_KEY,
				RSA.decode(serverPubKey, resp.getEncryptedKey()));

		LoginAuthReq loginReq = new LoginAuthReq(processor.getSession()
				.getAttribute(StringUtils.SECRET_KEY, byte[].class));
		loginReq.setUsername("zjw");
		loginReq.setPassword("aa");
		LoginAuthResp loginResp = (LoginAuthResp) processor.getSession()
				.sendWithResponse(loginReq);

		int i = 1;
		while (i-- > 0) {
			RemoteInterfaceMessageReq req = new RemoteInterfaceMessageReq();
			req.setInterfaceClass(UserInfoService.class.getName());
			req.setMethod("query");
			req.setParamClassList(int.class.getName());
			req.setParams(1);
			RemoteInterfaceMessageResp rmiResp = (RemoteInterfaceMessageResp) processor
					.getSession().sendWithResponse(req);
			SmartResult<UserInfoVO> result = (SmartResult<UserInfoVO>) rmiResp
					.getReturnObject();
			System.out.println(result);
		}
		client.shutdown();
	}
}
