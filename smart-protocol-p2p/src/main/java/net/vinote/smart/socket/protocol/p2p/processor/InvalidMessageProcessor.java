package net.vinote.smart.socket.protocol.p2p.processor;

import java.io.IOException;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 畸形消息处理器
 *
 * @author Seer
 * @version InvalidMessageProcessor.java, v 0.1 2015年3月16日 下午4:10:46 Seer Exp.
 */
public class InvalidMessageProcessor extends AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, DataEntry message) {
		RunLogger.getLogger().log(
				Level.SEVERE,
				"接受到畸形报文:" + session.getRemoteIp()
						+ StringUtils.toHexString(message.getData()));
		InvalidMessageReq msg = (InvalidMessageReq) message;
		InvalidMessageResp rspMsg = new InvalidMessageResp(msg.getHead());
		rspMsg.setMsg("畸形报文");
		rspMsg.setInvalidMsgData(msg.getInvalidMsgData());
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (IOException e) {
			session.invalidate();
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
		} catch (Exception e) {
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		session.invalidate(false);
	}

}
