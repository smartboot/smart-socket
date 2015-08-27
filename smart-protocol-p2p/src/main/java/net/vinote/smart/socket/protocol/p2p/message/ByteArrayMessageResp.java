package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

public class ByteArrayMessageResp extends BaseMessage {

	private byte[] byteArray;

	@Override
	protected void encodeBody() throws ProtocolException {
		writeBytes(byteArray);
	}

	@Override
	protected void decodeBody() {
		byteArray = readBytes();
	}

	public byte[] getByteArray() {
		return byteArray;
	}

	public void setByteArray(byte[] byteArray) {
		this.byteArray = byteArray;
	}

	@Override
	public int getMessageType() {
		return MessageType.BYTE_ARRAY_MESSAGE_RSP;
	}

}
