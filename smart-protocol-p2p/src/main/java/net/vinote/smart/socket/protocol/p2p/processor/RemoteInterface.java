package net.vinote.smart.socket.protocol.p2p.processor;

import java.util.List;

public interface RemoteInterface {
	RemoteModel say(RemoteModel name);

	public List<String> say1(RemoteModel name);
}
