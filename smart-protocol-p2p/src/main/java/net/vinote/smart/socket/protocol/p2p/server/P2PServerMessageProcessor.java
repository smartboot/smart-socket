package net.vinote.smart.socket.protocol.p2p.server;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.FragmentMessage;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageProcessor;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.service.session.SessionManager;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public class P2PServerMessageProcessor extends AbstractProtocolDataProcessor {
	private Logger logger = LoggerFactory.getLogger(P2PServerMessageProcessor.class);

	class ProcessUnit {
		String sessionId;
		BaseMessage msg;

		public ProcessUnit(String sessionId, BaseMessage msg) {
			this.sessionId = sessionId;
			this.msg = msg;
		}
	}

	private ProtocolProcessThread[] processThreads;
	private ArrayBlockingQueue<ProcessUnit> msgQueue;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.zjw.platform.quickly.process.MessageProcessor#process(com.zjw.
	 * platform .quickly.Session, com.zjw.platform.quickly.message.DataEntry)
	 */

	private int msgQueueMaxSize;

	public ClusterMessageEntry generateClusterMessage(ByteBuffer data) {
		ClusterMessageReq entry = new ClusterMessageReq();
		entry.setServiceData(data);
		return entry;
	}

	/*
	 * 处理消息 (non-Javadoc)
	 *
	 * @see com.zjw.platform.quickly.process.MessageProcessor#process(java.lang.
	 * Object )
	 */

	@Override
	public void init(QuicklyConfig config) throws Exception {
		super.init(config);
		msgQueueMaxSize = config.getThreadNum() * 500;
		msgQueue = new ArrayBlockingQueue<ProcessUnit>(msgQueueMaxSize);
		// 启动线程池处理消息
		processThreads = new P2PServerProcessThread[config.getThreadNum()];
		for (int i = 0; i < processThreads.length; i++) {
			processThreads[i] = new P2PServerProcessThread("OMC-Server-Process-Thread-" + i, this, msgQueue);
			processThreads[i].setPriority(Thread.MAX_PRIORITY);
			processThreads[i].start();
		}
		Properties properties = new Properties();
		properties.put(InvalidMessageReq.class.getName(), InvalidMessageProcessor.class.getName());
		config.getServiceMessageFactory().loadFromProperties(properties);
	}

	public <T> void process(T t) {
		ProcessUnit unit = (ProcessUnit) t;
		Session session = SessionManager.getInstance().getSession(unit.sessionId);
		if (session == null || session.isInvalid()) {
			logger.info("Session is invalid,lose message" + StringUtils.toHexString(unit.msg.getData().array()));
			return;
		}
		session.refreshAccessedTime();
		AbstractServiceMessageProcessor processor = getQuicklyConfig().getServiceMessageFactory().getProcessor(
			unit.msg.getClass());
		try {
			processor.processor(session, unit.msg);
		} catch (Exception e) {
			logger.warn("", e);
		}
	}

	public boolean receive(TransportSession tsession, ByteBuffer buffer) {
		// 会话封装并分配处理线程
		Session session = SessionManager.getInstance().getSession(tsession.getSessionID());
		if (session == null) {
			session = new P2PSession(tsession);
			SessionManager.getInstance().registSession(session);

		}
		session.refreshAccessedTime();
		FragmentMessage msg = session.getAttribute("FragmentMessage");
		if (msg == null) {
			msg = new FragmentMessage();
			session.setAttribute("FragmentMessage", msg);
		}
		msg.setData(buffer);
		BaseMessage baseMsg = msg.decodeMessage(getQuicklyConfig().getServiceMessageFactory());
		return session.notifySyncMessage(msg) ? true : msgQueue.offer(new ProcessUnit(session.getId(), baseMsg));
	}

	public void shutdown() {
		for (ProtocolProcessThread thread : processThreads) {
			thread.shutdown();
		}
		getQuicklyConfig().getServiceMessageFactory().destory();
	}

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
