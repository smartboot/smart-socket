package com.test.message;

import net.vinote.smart.socket.protocol.p2p.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.Session;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;

public class HelloWorldProcessor extends AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, BaseMessage message) {
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
