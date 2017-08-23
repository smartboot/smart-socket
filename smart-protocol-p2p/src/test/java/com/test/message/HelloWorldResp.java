package com.test.message;

import org.smartboot.socket.protocol.p2p.DecodeException;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.HeadMessage;
import org.smartboot.socket.protocol.p2p.message.MessageType;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class HelloWorldResp extends BaseMessage {

	public HelloWorldResp() {
		super();
	}

	public HelloWorldResp(HeadMessage head) {
		super(head);
	}

	private String say;

	@Override
	protected void encodeBody(ByteBuffer buffer) throws ProtocolException {
		writeString(buffer, say);
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws DecodeException {
		say = readString(buffer);
	}

	public String getSay() {
		return say;
	}

	public void setSay(String say) {
		this.say = say;
	}

	@Override
	public int getMessageType() {
		return MessageType.RESPONSE_MESSAGE | 0x01;
	}
}
