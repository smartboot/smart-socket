package net.vinote.smart.socket.protocol.p2p.processor;

import java.io.IOException;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.DetectMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 探测消息处理器
 * 
 * @author Seer
 *
 */
public class DetectMessageProcessor extends AbstractServiceMessageProcessor {

	public void processor(Session session, DataEntry message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		// RunLogger.getLogger().log(Level.FINE, message);
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setDetectMessage("探测响应消息" + msg.getHead().getSequenceID());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			session.invalidate();
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
		} catch (Exception e) {
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
	}

	public DataEntry processCluster(Session session, DataEntry message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setDetectMessage("集群探测响应消息" + msg.getHead().getSequenceID());
		return rspMsg;
	}

}
