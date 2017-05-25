package net.vinote.smart.socket.protocol.p2p.client;

import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
import net.vinote.smart.socket.transport.TransportSession;

public class P2PClientMessageProcessor extends AbstractClientDataProcessor<BaseMessage> {

	@Override
	public void process(Session<BaseMessage> session, BaseMessage msg) throws Exception {
		// System.out.println(msg);
	}

	@Override
	public void initChannel(TransportSession<BaseMessage> session) {
		this.session = new P2PSession(session);
	}

}
