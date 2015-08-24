package com.test.message;

import java.net.ProtocolException;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.protocol.p2p.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.HeadMessage;
import net.vinote.smart.socket.protocol.p2p.MessageType;

public class HelloWorldResp extends BaseMessage {

	public HelloWorldResp() {
		super();
	}

	public HelloWorldResp(HeadMessage head) {
		super(head);
	}

	private String say;

	@Override
	protected void encodeBody() throws ProtocolException {
		writeString(say);
	}

	@Override
	protected void decodeBody() throws DecodeException {
		say = readString();
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
