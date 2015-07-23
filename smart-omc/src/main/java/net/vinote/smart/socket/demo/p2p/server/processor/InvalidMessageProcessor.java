package net.vinote.smart.socket.demo.p2p.server.processor;

import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 畸形消息处理器
 * 
 * @author Seer
 *
 */
public class InvalidMessageProcessor extends AbstractServiceMessageProcessor {
	private static final RunLogger logger = RunLogger.getLogger();

	public void processor(Session session, DataEntry message) {
		logger.log(Level.SEVERE, "接受到畸形报文:" + session.getRemoteIp() + StringUtils.toHexString(message.getData()));
		session.invalidate();
	}

}
