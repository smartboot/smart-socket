package net.vinote.smart.socket.protocol;

/**
 * P2P协议工厂
 *
 * @author Seer
 * @version P2PProtocolFactory.java, v 0.1 2015年8月24日 上午10:07:47 Seer Exp.
 */
public class P2PProtocolFactory implements ProtocolFactory {
	private Protocol protocol;

	public Protocol createProtocol() {
		if (protocol != null) {
			return protocol;
		}
		synchronized (P2PProtocolFactory.class) {
			if (protocol == null) {
				protocol = new P2PProtocol();
			}
		}
		return protocol;
	}

}
