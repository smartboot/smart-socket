package net.vinote.smart.socket.protocol.p2p.processor;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 畸形报文响应消息处理器
 *
 * @author Seer
 * @version InvalidMessageResponseProcessor.java, v 0.1 2015年3月16日 下午4:11:17
 *          Seer Exp.
 */
public class InvalidMessageResponseProcessor extends AbstractServiceMessageProcessor {
	private Logger logger = LoggerFactory.getLogger(InvalidMessageResponseProcessor.class);

	@Override
	public void processor(Session session, DataEntry message) {
		logger.info("接受到畸形报文响应消息:" + session.getRemoteIp() + StringUtils.toHexString(message.getData().array()));
	}

}
