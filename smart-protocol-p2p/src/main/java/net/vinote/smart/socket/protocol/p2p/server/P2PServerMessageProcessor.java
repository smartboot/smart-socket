package net.vinote.smart.socket.protocol.p2p.server;

import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public final class P2PServerMessageProcessor extends AbstractServerDataGroupProcessor<BaseMessage> {
	private P2pServiceMessageFactory serviceMessageFactory;

	public P2PServerMessageProcessor(P2pServiceMessageFactory serviceMessageFactory) {
		this.serviceMessageFactory = serviceMessageFactory;
	}

	@Override
	public void process(Session<BaseMessage> session, BaseMessage entry) throws Exception {
		MessageHandler handler = serviceMessageFactory.getProcessor(entry.getClass());
		handler.handler(session, entry);
	}

	@Override
	public Session<BaseMessage> initSession(TransportSession<BaseMessage> session) {
		return new P2PSession(session);
	}
}
