package com.gaoyiping.demo;

import java.nio.ByteBuffer;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

public class DemoProtocol implements Protocol<byte[]> {

	public byte[] decode(ByteBuffer readBuffer, AioSession session) {
		if (readBuffer.remaining() > 0) {
			byte[] data = new byte[readBuffer.remaining()];
			readBuffer.get(data);
			return data;
			// type 1,2,3 message, see:
			// https://smartboot.gitee.io/docs/smart-socket/second/3-type-one.html
			// https://smartboot.gitee.io/docs/smart-socket/second/4-type-two.html
			// https://smartboot.gitee.io/docs/smart-socket/second/5-type-three.html
		}
		return null;
	}

	public ByteBuffer encode(byte[] msg, AioSession session) {
		ByteBuffer buffer = ByteBuffer.allocate(msg.length);
		buffer.put(msg);
		buffer.flip();
		return buffer;
	}
}
