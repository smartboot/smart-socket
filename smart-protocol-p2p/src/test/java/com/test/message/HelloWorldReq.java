package com.test.message;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.MessageType;

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
		buffer.putInt(age);
		writeBoolean(buffer, male);
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws DecodeException {
		name = readString(buffer);
		age = buffer.getInt();
		male = readBoolen(buffer);
	}

	@Override
	public int getMessageType() {
		return MessageType.REQUEST_MESSAGE | 0x01;
	}
}
