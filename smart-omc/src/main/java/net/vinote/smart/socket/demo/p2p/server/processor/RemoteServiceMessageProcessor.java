package net.vinote.smart.socket.demo.p2p.server.processor;

import java.util.HashMap;
import java.util.Map;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

public class RemoteServiceMessageProcessor extends
		AbstractServiceMessageProcessor {
	private Map<String, Object> impMap = new HashMap<String, Object>();

	@Override
	public void init() {
		// TODO Auto-generated method stub
		super.init();
	}

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		RemoteInterfaceMessageReq req = (RemoteInterfaceMessageReq) message;
		Object impObj = impMap.get(req.getUniqueId() + ":"
				+ req.getInterfaceClass());
		Object obj = impObj.getClass().getMethod(req.getMethod())
				.invoke(impObj);
		RemoteInterfaceMessageResp resp = new RemoteInterfaceMessageResp();
		session.sendWithoutResponse(resp);
	}
}
