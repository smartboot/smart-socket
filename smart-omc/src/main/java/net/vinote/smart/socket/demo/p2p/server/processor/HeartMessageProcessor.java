package net.vinote.smart.socket.demo.p2p.server.processor;

import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.HeadMessage;
import net.vinote.smart.socket.protocol.p2p.HeartMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 心跳消息处理器
 * 
 * @author Seer
 *
 */
public class HeartMessageProcessor extends AbstractServiceMessageProcessor {
	private static final RunLogger logger = RunLogger.getLogger();

	public void processor(Session session, DataEntry message) {
		HeadMessage head = new HeadMessage();
		HeartMessageResp rspMsg = new HeartMessageResp();
		rspMsg.setHead(head);
		try {
			session.sendWithoutResponse(rspMsg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.log(Level.SEVERE, StringUtils.toHexString(message.getData()));
	}

}
