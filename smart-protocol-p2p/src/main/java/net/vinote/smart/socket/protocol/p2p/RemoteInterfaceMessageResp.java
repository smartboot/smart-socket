package net.vinote.smart.socket.protocol.p2p;

import java.net.ProtocolException;

public class RemoteInterfaceMessageResp extends BaseMessage {

	public RemoteInterfaceMessageResp() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RemoteInterfaceMessageResp(HeadMessage head) {
		super(head);
		// TODO Auto-generated constructor stub
	}

	/** 出参序列化流 */
	private byte[] outParameter;

	@Override
	protected void encodeBody() throws ProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decodeBody() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMessageType() {
		return MessageType.REMOTE_INTERFACE_MESSAGE_RSP;
	}

}
