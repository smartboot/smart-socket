package net.vinote.smart.socket.protocol.p2p.server;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.transport.TransportSession;

final class ProcessUnit {
		TransportSession<BaseMessage> session;
		BaseMessage msg;

		public ProcessUnit(TransportSession<BaseMessage> session, BaseMessage msg) {
			this.session = session;
			this.msg = msg;
		}
	}