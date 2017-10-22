package com.test.message;

import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.Session;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;

public class HelloWorldHandler extends MessageHandler {

	@Override
	public void handler(Session<BaseMessage> session, BaseMessage message) {
		HelloWorldReq request = (HelloWorldReq) message;
		HelloWorldResp resp = new HelloWorldResp(request.getHead());
		resp.setSay(request.getName() + " say: Hello World,I'm " + request.getAge() + " years old. I'm a "
				+ (request.isMale() ? "boy" : "girl"));
		try {
			session.sendWithoutResponse(resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
