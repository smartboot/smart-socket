package com.test.message;

import org.smartboot.socket.protocol.p2p.DecodeException;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.MessageType;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class HelloWorldReq extends BaseMessage {

	private String name;
	private int age;
	private boolean male;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isMale() {
		return male;
	}

	public void setMale(boolean male) {
		this.male = male;
	}

	@Override
	protected void encodeBody(ByteBuffer buffer) throws ProtocolException {
		writeString(buffer, name);
		writeInt(buffer, age);
		writeBoolean(buffer, male);
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws DecodeException {
		name = readString(buffer);
		age = readInt(buffer);
		male = readBoolen(buffer);
	}

	@Override
	public int getMessageType() {
		return MessageType.REQUEST_MESSAGE | 0x01;
	}
}
