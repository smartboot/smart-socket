package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 请求公钥
 * 
 * @author Seer
 * @version PublicKeyReq.java, v 0.1 2015年8月26日 下午5:59:24 Seer Exp.
 */
public class PublicKeyResp extends BaseMessage {
	private byte[] publicKey;

	public PublicKeyResp() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PublicKeyResp(HeadMessage head) {
		super(head);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void encodeBody() throws ProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decodeBody() throws DecodeException {
		// TODO Auto-generated method stub

	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public int getMessageType() {
		// TODO Auto-generated method stub
		return 0;
	}
}
