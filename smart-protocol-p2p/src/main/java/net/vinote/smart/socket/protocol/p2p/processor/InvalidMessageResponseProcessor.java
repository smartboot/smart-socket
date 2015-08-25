package net.vinote.smart.socket.protocol.p2p.processor;

import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 畸形报文响应消息处理器
 *
 * @author Seer
 * @version InvalidMessageResponseProcessor.java, v 0.1 2015年3月16日 下午4:11:17
 *          Seer Exp.
 */
public class InvalidMessageResponseProcessor extends
		AbstractServiceMessageProcessor {

	@Override
	public void processor(Session session, DataEntry message) {
		RunLogger.getLogger().log(
				Level.SEVERE,
				"接受到畸形报文响应消息:" + session.getRemoteIp()
						+ StringUtils.toHexString(message.getData()));
	}

}
