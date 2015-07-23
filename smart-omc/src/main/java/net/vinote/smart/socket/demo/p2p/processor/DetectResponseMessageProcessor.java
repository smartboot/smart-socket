package net.vinote.smart.socket.demo.p2p.processor;

import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 探测消息处理器
 * 
 * @author Seer
 *
 */
public class DetectResponseMessageProcessor extends AbstractServiceMessageProcessor {
	private static final RunLogger logger = RunLogger.getLogger();

	public void processor(Session session, DataEntry message) {
		logger.log(Level.WARNING, "超时探测消息响应" + StringUtils.toHexString(message.getData()));
	}
}
