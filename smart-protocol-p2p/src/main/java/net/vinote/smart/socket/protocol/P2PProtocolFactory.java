package net.vinote.smart.socket.protocol;

/**
 * P2P协议工厂
 * 
 * @author Seer
 * @version P2PProtocolFactory.java, v 0.1 2015年8月24日 上午10:07:47 Seer Exp.
 */
public class P2PProtocolFactory implements ProtocolFactory {

	public Protocol createProtocol() {
		return new P2PProtocol();
	}

}
