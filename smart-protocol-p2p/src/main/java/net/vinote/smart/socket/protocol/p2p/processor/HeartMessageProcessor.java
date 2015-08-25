package net.vinote.smart.socket.protocol.p2p.processor;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.HeartMessageReq;
import net.vinote.smart.socket.protocol.p2p.HeartMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 心跳消息处理器
 * 
 * @author Seer
 *
 */
public class HeartMessageProcessor extends AbstractServiceMessageProcessor {

	public void processor(Session session, DataEntry message) {
		HeartMessageReq req = (HeartMessageReq) message;
		HeartMessageResp rspMsg = new HeartMessageResp(req.getHead());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (Exception e) {
			RunLogger.getLogger().log(e);
		}
	}

}
