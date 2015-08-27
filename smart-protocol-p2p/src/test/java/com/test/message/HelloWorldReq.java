package com.test.message;

import java.net.ProtocolException;

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
	protected void encodeBody() throws ProtocolException {
		writeString(name);
		writeInt(age);
		writeBoolean(male);
	}

	@Override
	protected void decodeBody() throws DecodeException {
		name = readString();
		age = readInt();
		male = readBoolen();
	}

	@Override
	public int getMessageType() {
		return MessageType.REQUEST_MESSAGE | 0x01;
	}
}
