package org.smartboot.socket.protocol.p2p.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.p2p.MessageHandler;
import org.smartboot.socket.protocol.p2p.Session;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;

import java.io.IOException;

/**
 * 探测消息处理器
 *
 * @author Seer
 *
 */
public class DetectMessageHandler extends MessageHandler {
	private static Logger logger = LogManager.getLogger(DetectMessageHandler.class);

	@Override
	public void handler(Session<BaseMessage> session, BaseMessage message) {
		DetectMessageReq msg = (DetectMessageReq) message;
		DetectMessageResp rspMsg = new DetectMessageResp(msg.getHead());
		rspMsg.setSendTime(msg.getSendTime());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
