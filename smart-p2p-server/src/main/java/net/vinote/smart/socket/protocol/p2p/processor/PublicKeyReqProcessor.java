package net.vinote.smart.socket.protocol.p2p.processor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.PublicKeyReq;
import net.vinote.smart.socket.protocol.p2p.PublicKeyResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

public class PublicKeyReqProcessor extends AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		PublicKeyReq req = (PublicKeyReq) message;
		KeyPairGenerator generator = KeyPairGenerator.getInstance(req
				.getAlgorithm());
		KeyPair keyPair = generator.generateKeyPair();
		session.setAttribute("PrivateKey", keyPair.getPrivate());

		PublicKeyResp resp = new PublicKeyResp(req.getHead());
		resp.setPublicKey(keyPair.getPublic().getEncoded());
		session.sendWithoutResponse(resp);
	}
}
