package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 请求公钥
 * 
 * @author Seer
 * @version PublicKeyReq.java, v 0.1 2015年8月26日 下午5:59:24 Seer Exp.
 */
public class PublicKeyReq extends BaseMessage {
	private String algorithm;

	@Override
	protected void encodeBody() throws ProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decodeBody() throws DecodeException {
		// TODO Auto-generated method stub

	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public int getMessageType() {
		// TODO Auto-generated method stub
		return 0;
	}
}
