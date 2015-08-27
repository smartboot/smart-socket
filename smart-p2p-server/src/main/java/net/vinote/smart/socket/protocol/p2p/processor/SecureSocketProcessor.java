package net.vinote.smart.socket.protocol.p2p.processor;

import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.KeyGenerator;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketReq;
import net.vinote.smart.socket.protocol.p2p.message.SecureSocketResp;
import net.vinote.smart.socket.security.RSA;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 安全通信处理器;采用非对称加密算法RSA进行双向认证,并生成对称加密算法AES128秘钥传输至对端
 * 
 * 
 * @author Seer
 * @version SecureSocketReqProcessor.java, v 0.1 2015年8月27日 下午3:49:45 Seer Exp.
 */
public class SecureSocketProcessor extends AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		SecureSocketReq req = (SecureSocketReq) message;
		// 获取客户端的公钥
		PublicKey pubKey = RSA.generatePublicKey(req.getRsaPublicKey());

		// 生成服务端的秘钥对
		KeyPair keyPair = RSA.generateKeyPair();
		byte[] encodeServerPubKey = RSA.encode(pubKey, keyPair.getPublic()
				.getEncoded());// 使用客户端的公钥加密服务端的公钥
		SecureSocketResp resp = new SecureSocketResp(req.getHead());
		resp.setRsaPublicKey(encodeServerPubKey);

		// 用服务端私钥加密对称加密秘钥
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		byte[] secretKey = kgen.generateKey().getEncoded();
		resp.setEncryptedKey(RSA.encode(keyPair.getPrivate(), secretKey));// 使用服务端的私钥加密对称加密秘钥
		session.setAttribute(StringUtils.SECRET_KEY, secretKey);
		session.sendWithoutResponse(resp);
	}

}
