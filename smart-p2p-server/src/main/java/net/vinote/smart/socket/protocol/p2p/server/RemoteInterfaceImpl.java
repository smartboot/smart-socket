package net.vinote.smart.socket.protocol.p2p.server;

import java.util.List;

public class RemoteInterfaceImpl implements RemoteInterface {

	@Override
	public RemoteModel say(RemoteModel name) {
		RemoteModel model = new RemoteModel();
		model.setName(name.getName() + " say:Hello World");
		return model;
	}

	@Override
	public List<String> say1(RemoteModel name) {
		RemoteModel model = new RemoteModel();
		model.setName(name.getName() + " say:Hello World");
		return model.getList();
	}
}
