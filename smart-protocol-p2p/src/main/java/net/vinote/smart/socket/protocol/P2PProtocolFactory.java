package net.vinote.smart.socket.protocol;


public class P2PProtocolFactory implements ProtocolFactory {

	
	public Protocol createProtocol() {
		return new P2PProtocol();
	}

}
