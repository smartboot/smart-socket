package net.vinote.smart.socket.protocol.p2p.server;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageProcessor;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractProtocolDisruptorProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.service.session.SessionManager;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public class P2PServerDisruptorProcessor extends AbstractProtocolDisruptorProcessor {

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		ClusterMessageReq entry = new ClusterMessageReq();
		entry.setServiceData(data);
		return entry;
	}

	public void init(QuicklyConfig config) throws Exception {
		super.init(config);
		// 启动线程池处理消息
		Properties properties = new Properties();
		properties.put(InvalidMessageReq.class.getName(), InvalidMessageProcessor.class.getName());
		BaseMessageFactory.getInstance().loadFromProperties(properties);
	}

	public <T> void process(T t) {
		ProcessUnit unit = (ProcessUnit) t;
		Session session = SessionManager.getInstance().getSession(unit.sessionId);
		try {
			if (session == null || session.isInvalid()) {
				RunLogger.getLogger().log(Level.FINEST,
					"Session is invalid,lose message" + StringUtils.toHexString(unit.msg.getData()));
				return;
			}
			session.refreshAccessedTime();
			AbstractServiceMessageProcessor processor = ServiceProcessorManager.getInstance().getProcessor(
				unit.msg.getClass());
			processor.processor(session, unit.msg);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void shutdown() {
		super.shutdown();
		ServiceProcessorManager.getInstance().destory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.vinote.smart.socket.service.process.ProtocolDataProcessor#getSession
	 * (net.vinote.smart.socket.transport.TransportSession)
	 */
	@Override
	public Session getSession(TransportSession tsession) {
		Session session = SessionManager.getInstance().getSession(tsession.getSessionID());
		if (session == null) {
			session = new P2PSession(tsession);
			SessionManager.getInstance().registSession(session);
		}
		session.refreshAccessedTime();
		return session;
	}
}
