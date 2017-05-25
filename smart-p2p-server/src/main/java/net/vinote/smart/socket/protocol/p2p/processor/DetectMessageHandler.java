package net.vinote.smart.socket.protocol.p2p.processor;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.vinote.smart.socket.protocol.p2p.MessageHandler;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.DetectMessageResp;
import net.vinote.smart.socket.service.Session;

/**
 * 探测消息处理器
 *
 * @author Seer
 *
 */
public class DetectMessageHandler extends MessageHandler {
	private Logger logger = LogManager.getLogger(DetectMessageHandler.class);

	@Override
	public void handler(Session<BaseMessage> session, BaseMessage message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setDetectMessage("探测响应消息" + msg.getHead().getSequenceID());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
