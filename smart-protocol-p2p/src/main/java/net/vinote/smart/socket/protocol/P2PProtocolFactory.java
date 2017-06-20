package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;

/**
 * P2P协议工厂
 *
 * @author Seer
 * @version P2PProtocolFactory.java, v 0.1 2015年8月24日 上午10:07:47 Seer Exp.
 */
public class P2PProtocolFactory implements ProtocolFactory<BaseMessage> {
	private Protocol<BaseMessage> protocol;
	private P2pServiceMessageFactory serviceMessageFactory;

	public P2PProtocolFactory(P2pServiceMessageFactory serviceMessageFactory) {
		this.serviceMessageFactory = serviceMessageFactory;
	}

	public Protocol<BaseMessage> createProtocol() {
		if (protocol != null) {
			return protocol;
		}
		synchronized (P2PProtocolFactory.class) {
			if (protocol == null) {
				protocol = new P2PProtocol(serviceMessageFactory);
			}
		}
		return protocol;
	}

}
