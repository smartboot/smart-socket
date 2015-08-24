package com.test.message;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

public class HelloWorldProcessor extends AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		HelloWorldReq request = (HelloWorldReq) message;
		HelloWorldResp resp = new HelloWorldResp(request.getHead());
		resp.setSay(request.getName() + " say: Hello World,I'm "
				+ request.getAge() + " years old. I'm a "
				+ (request.isMale() ? "boy" : "girl"));
		session.sendWithoutResponse(resp);
	}
}
