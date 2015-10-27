package net.vinote.smart.socket.protocol.p2p.processor;

import java.io.IOException;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 畸形消息处理器
 *
 * @author Seer
 * @version InvalidMessageProcessor.java, v 0.1 2015年3月16日 下午4:10:46 Seer Exp.
 */
public class InvalidMessageProcessor extends AbstractServiceMessageProcessor {
	private Logger logger = LoggerFactory.getLogger(InvalidMessageProcessor.class);

	@Override
	public void processor(Session session, DataEntry message) {
		logger.info("接受到畸形报文:" + session.getRemoteIp() + StringUtils.toHexString(message.getData()));
		InvalidMessageReq msg = (InvalidMessageReq) message;
		InvalidMessageResp rspMsg = new InvalidMessageResp(msg.getHead());
		rspMsg.setMsg("畸形报文");
		rspMsg.setInvalidMsgData(msg.getInvalidMsgData());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			session.invalidate();
			logger.warn(e.getMessage(), e);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		session.invalidate(false);
	}

}
