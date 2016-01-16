package net.vinote.smart.socket.protocol.p2p;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;

public abstract class AbstractServiceMessageProcessor {
	public void init() {
	}

	public abstract void processor(Session session, BaseMessage message);

	public void destory() {
	}
}
