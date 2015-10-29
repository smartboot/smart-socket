package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.FragmentMessage;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageResp;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageResponseProcessor;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.process.ClientProcessor;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PClientMessageProcessor extends AbstractProtocolDataProcessor implements ClientProcessor {
	private Logger logger = LoggerFactory.getLogger(P2PClientMessageProcessor.class);
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
		config.getServiceMessageFactory().loadFromProperties(properties);
	}

	public <T> void process(T msg) {
		// 获取处理器
		BaseMessage message = (BaseMessage) msg;
		AbstractServiceMessageProcessor processor = getQuicklyConfig().getServiceMessageFactory().getProcessor(
			message.getClass());
		if (processor != null) {
			try {
				processor.processor(session, message);
			} catch (Exception e) {
				logger.warn("", e);
			}
		} else {
			// logger.info("unsupport message" +
			// StringUtils.toHexString(message.getData()));
		}
	}

	public boolean receive(TransportSession tsession, DataEntry msg) {
		BaseMessage baseMsg = ((FragmentMessage) msg).decodeMessage(getQuicklyConfig().getServiceMessageFactory());
		if (baseMsg == null) {
			throw new DecodeException("Decode Message Error!" + StringUtils.toHexString(msg.getData()));
		}
		ensureSession(tsession);

		// 解密消息
		if (baseMsg.getHead().isSecure()) {
			baseMsg.getHead().setSecretKey(session.getAttribute(StringUtils.SECRET_KEY, byte[].class));
			baseMsg.decode();
		}

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
		getQuicklyConfig().getServiceMessageFactory().destory();
	}
}
