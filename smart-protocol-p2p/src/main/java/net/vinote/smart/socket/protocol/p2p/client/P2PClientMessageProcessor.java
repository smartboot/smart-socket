package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
import net.vinote.smart.socket.io.Channel;

public class P2PClientMessageProcessor extends AbstractClientDataProcessor<BaseMessage> {
	private P2pServiceMessageFactory serviceMessageFactory;

	public P2PClientMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
		this.serviceMessageFactory = serviceMessageFactory;
	}
	@Override
	public void process(Session<BaseMessage> session, BaseMessage msg) throws Exception {
		MessageHandler handler = serviceMessageFactory.getProcessor(msg.getClass());
		handler.handler(session, msg);
	}

	@Override
	public Session<BaseMessage> initSession(Channel<BaseMessage> session) {
		this.session = new P2PSession(session);
		return this.session;
	}

}
