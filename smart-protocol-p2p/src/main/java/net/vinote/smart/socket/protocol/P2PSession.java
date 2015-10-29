package net.vinote.smart.socket.protocol;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.MessageType;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Seer
 *
 */
public class P2PSession implements Session {
	private Logger logger = LoggerFactory.getLogger(P2PSession.class);
	private String remoteIp;
	private String localAddress;
	private TransportSession session;
	/** 会话创建时间 */
	private final long creatTime;
	/** 上一次访问时间 */
	private long lastAccessTime;
	/** 最长闲置时间 */
	private int maxInactiveInterval;
	/** 当前会话唯一标识 */
	private String sessionId = null;
	/** 失效标识 */
	private boolean invalidated = false;
	private Map<String, BaseMessage> synchRespMap;

	private Map<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

	public P2PSession(TransportSession session) {
		sessionId = session.getSessionID();
		remoteIp = session.getRemoteAddr();
		localAddress = session.getLocalAddress();
		this.session = session;
		maxInactiveInterval = session.getQuickConfig().getTimeout();
		synchRespMap = new ConcurrentHashMap<String, BaseMessage>();
		creatTime = System.currentTimeMillis();
		refreshAccessedTime();
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		refreshAccessedTime();
		return (T) attributeMap.get(name);
	}

	public long getCreationTime() {
		return creatTime;
	}

	public String getId() {
		refreshAccessedTime();
		return sessionId;
	}

	public long getLastAccessedTime() {
		return lastAccessTime;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void invalidate(boolean immediate) {

		invalidated = true;
		for (BaseMessage unit : synchRespMap.values()) {
			synchronized (unit) {
				unit.notifyAll();
			}
		}
		attributeMap.clear();
		session.close(immediate);
	}

	public void invalidate() {
		invalidate(true);
	}

	public void removeAttribute(String name) {
		// TODO Auto-generated method stub

	}

	public void setAttribute(String name, Object value) {
		refreshAccessedTime();
		attributeMap.put(name, value);
	}

	public void setMaxInactiveInterval(int interval) {
		refreshAccessedTime();
		maxInactiveInterval = interval;
	}

	@Override
	public String toString() {
		return "OMCSession [remoteIp=" + remoteIp + ", session=" + session + ", creatTime=" + creatTime
			+ ", lastAccessTime=" + lastAccessTime + ", maxInactiveInterval=" + maxInactiveInterval + ", sessionId="
			+ sessionId + ", invalidated=" + invalidated + "]";
	}

	public boolean isInvalid() {
		return invalidated;
	}

	private void assertTransactionSession() throws IOException {
		if (session == null || !session.isValid()) {
			throw new IOException("Socket Channel is invalid now");
		}
	}

	public void refreshAccessedTime() {
		lastAccessTime = System.currentTimeMillis();
	}

	public String getLocalAddress() {
		return localAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.zjw.platform.quickly.service.session.Session#notifySyncMessage(com
	 * .zjw.platform.quickly.protocol.DataEntry)
	 */

	public boolean notifySyncMessage(DataEntry baseMsg) {
		BaseMessage respMsg = (BaseMessage) baseMsg;
		if (isRequestMessage(respMsg.getMessageType())) {
			return false;
		}
		String sequenceId = String.valueOf(respMsg.getHead().getSequenceID());
		BaseMessage reqMsg = synchRespMap.get(sequenceId);
		if (reqMsg != null) {
			synchronized (reqMsg) {
				if (synchRespMap.containsKey(sequenceId)) {
					synchRespMap.put(sequenceId, respMsg);
					reqMsg.notifyAll();
					return true;
				}
			}
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.zjw.platform.quickly.service.session.Session#sendWithoutResponse(
	 * com.zjw.platform.quickly.protocol.DataEntry)
	 */

	public void sendWithoutResponse(DataEntry requestMsg) throws Exception {
		BaseMessage reqMsg = (BaseMessage) requestMsg;
		assertTransactionSession();
		refreshAccessedTime();
		session.write(reqMsg);

	}

	@Override
	public DataEntry sendWithResponse(DataEntry requestMsg, long timeout) throws Exception {
		BaseMessage reqMsg = (BaseMessage) requestMsg;
		assertTransactionSession();
		refreshAccessedTime();

		if (!isRequestMessage(reqMsg.getMessageType())) {
			throw new InvalidParameterException("current message is not a requestMessage, messageType is 0x"
				+ Integer.toHexString(reqMsg.getMessageType()));
		}
		reqMsg.encode();// 必须执行encode才可产生sequenceId
		String sequenceId = String.valueOf(reqMsg.getHead().getSequenceID());
		synchRespMap.put(sequenceId, reqMsg);
		session.write(reqMsg);
		if (synchRespMap.containsKey(sequenceId) && synchRespMap.get(sequenceId) == reqMsg) {
			synchronized (reqMsg) {
				if (synchRespMap.containsKey(sequenceId) && synchRespMap.get(sequenceId) == reqMsg) {
					try {
						reqMsg.wait(timeout);
					} catch (InterruptedException e) {
						logger.warn("", e);
					}
				}
			}
		}
		BaseMessage resp = null;
		synchronized (reqMsg) {
			resp = synchRespMap.remove(sequenceId);
		}
		if (resp == null || resp == reqMsg) {
			throw new SocketTimeoutException("Message " + sequenceId + " is timeout!");
		}
		return resp;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.vinote.smart.socket.service.session.Session#sendWithResponse(net.
	 * vinote.smart.socket.protocol.DataEntry)
	 */
	public DataEntry sendWithResponse(DataEntry requestMsg) throws Exception {
		return sendWithResponse(requestMsg, session.getTimeout());
	}

	/**
	 * 是否为请求消息类型
	 *
	 * @param msgType
	 * @return
	 */
	private boolean isRequestMessage(int msgType) {
		return (MessageType.RESPONSE_MESSAGE & msgType) == MessageType.REQUEST_MESSAGE;
	}

	@Override
	public TransportSession getTransportSession() {
		return session;
	}

}
