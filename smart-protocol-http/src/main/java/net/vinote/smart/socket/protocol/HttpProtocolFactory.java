package net.vinote.smart.socket.protocol;

public class HttpProtocolFactory implements ProtocolFactory {

	
	public Protocol createProtocol() {
		return new HttpProtocol();
	}

}
