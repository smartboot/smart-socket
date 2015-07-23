package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.InvalidMessageResp;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageResponseProcessor;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.process.ClientProcessor;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

public class P2PClientMessageProcessor extends AbstractProtocolDataProcessor implements ClientProcessor {
	private Session session;
	private P2PClientProcessThread processThread;

	public void createSession(TransportSession session) {
		ensureSession(session);
	}

	/**
	 * 确保当前存在Session
	 *
	 * @param tsession
	 */
	private void ensureSession(TransportSession tsession) {
		if (session == null) {
			synchronized (this) {
				if (session == null) {
					processThread = new P2PClientProcessThread("P2PClient-Thread", this);
					processThread.start();
					session = new P2PSession(tsession);
				}
			}
		}
	}

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		return null;
	}

	public final Session getSession() {
		return session;
	}

	@Override
	public void init(QuicklyConfig config) throws Exception {
		super.init(config);
		Properties properties = new Properties();
		properties.put(InvalidMessageResp.class.getName(), InvalidMessageResponseProcessor.class.getName());
		BaseMessageFactory.getInstance().loadFromProperties(properties);
	}

	public <T> void process(T msg) {
		// 获取处理器
		BaseMessage message = (BaseMessage) msg;
		AbstractServiceMessageProcessor processor = ServiceProcessorManager.getInstance().getProcessor(
			message.getClass());
		if (processor != null) {
			try {
				processor.processor(session, message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			RunLogger.getLogger().log(Level.SEVERE, "unsupport message" + StringUtils.toHexString(message.getData()));
		}
	}

	public boolean receive(TransportSession tsession, DataEntry msg) {
		BaseMessage baseMsg = (BaseMessage) msg;
		ensureSession(tsession);
		// 服务器返回的非响应消息交由专门的处理器处理
		if (!session.notifySyncMessage(baseMsg)) {
			// 同步响应消息若出现超时情况,也会进到if里面
			processThread.put(session.getId(), (BaseMessage) msg);
		}
		return true;
	}

	public void shutdown() {
		if (session != null) {
			session.invalidate();
			processThread.shutdown();
		}
		ServiceProcessorManager.getInstance().destory();
	}
}
