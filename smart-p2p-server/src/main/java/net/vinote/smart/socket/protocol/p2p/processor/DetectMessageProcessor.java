package net.vinote.smart.socket.protocol.p2p.processor;

import java.io.IOException;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 探测消息处理器
 *
 * @author Seer
 *
 */
public class DetectMessageProcessor extends AbstractServiceMessageProcessor {
	private Logger logger = LoggerFactory.getLogger(DetectMessageProcessor.class);

	@Override
	public void processor(Session session, DataEntry message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setDetectMessage("探测响应消息" + msg.getHead().getSequenceID());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			session.invalidate();
			logger.warn(e.getMessage(), e);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}

	@Override
	public DataEntry processCluster(Session session, DataEntry message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setDetectMessage("集群探测响应消息" + msg.getHead().getSequenceID());
		return rspMsg;
	}

}
