package net.vinote.smart.socket.protocol.p2p.server;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageProcessor;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.process.ProtocolProcessThread;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.service.session.SessionManager;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public class P2PServerMessageProcessor extends AbstractProtocolDataProcessor {
	class ProcessUnit {
		String sessionId;
		BaseMessage msg;

		public ProcessUnit(String sessionId, BaseMessage msg) {
			this.sessionId = sessionId;
			this.msg = msg;
		}
	}

	private static final RunLogger logger = RunLogger.getLogger();
	private ProtocolProcessThread[] processThreads;
	private ArrayBlockingQueue<ProcessUnit> msgQueue;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.zjw.platform.quickly.process.MessageProcessor#process(com.zjw.platform
	 * .quickly.Session, com.zjw.platform.quickly.message.DataEntry)
	 */

	private int msgQueueMaxSize;

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		ClusterMessageReq entry = new ClusterMessageReq();
		entry.setServiceData(data);
		return entry;
	}

	/*
	 * 处理消息 (non-Javadoc)
	 *
	 * @see
	 * com.zjw.platform.quickly.process.MessageProcessor#process(java.lang.Object
	 * )
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
		BaseMessageFactory.getInstance().loadFromProperties(properties);
	}

	public <T> void process(T t) {
		ProcessUnit unit = (ProcessUnit) t;
		Session session = SessionManager.getInstance().getSession(unit.sessionId);
		if (session == null || session.isInvalid()) {
			logger.log(Level.FINEST, "Session is invalid,lose message" + StringUtils.toHexString(unit.msg.getData()));
			return;
		}
		session.refreshAccessedTime();
		AbstractServiceMessageProcessor processor = ServiceProcessorManager.getInstance().getProcessor(
			unit.msg.getClass());
		try {
			processor.processor(session, unit.msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean receive(TransportSession tsession, DataEntry msg) {
		// 会话封装并分配处理线程
		Session session = SessionManager.getInstance().getSession(tsession.getSessionID());
		if (session == null) {
			session = new P2PSession(tsession);
			SessionManager.getInstance().registSession(session);
		}
		session.refreshAccessedTime();
		return session.notifySyncMessage(msg) ? true : msgQueue.offer(new ProcessUnit(session.getId(),
			(BaseMessage) msg));
	}

	public void shutdown() {
		for (ProtocolProcessThread thread : processThreads) {
			thread.shutdown();
		}
		ServiceProcessorManager.getInstance().destory();
	}
}
