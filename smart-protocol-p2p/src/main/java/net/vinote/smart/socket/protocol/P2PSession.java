package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.MessageType;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.io.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Seer
 */
public class P2PSession implements Session<BaseMessage> {
    private static Logger logger = LogManager.getLogger(P2PSession.class);
    private String remoteIp;
    private String localAddress;
    private Channel<BaseMessage> session;
    /**
     * 会话创建时间
     */
    private final long creatTime;
    /**
     * 最长闲置时间
     */
    private int maxInactiveInterval;
    /**
     * 当前会话唯一标识
     */
    private String sessionId = null;
    /**
     * 失效标识
     */
    private boolean invalidated = false;
    private Map<Integer, BaseMessage> synchRespMap;

    private Map<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

    public P2PSession(Channel<BaseMessage> session) {
        sessionId = session.getSessionID();
//		remoteIp = session.getRemoteAddr();
//		localAddress = session.getLocalAddress();
        this.session = session;
        maxInactiveInterval = session.getTimeout();
        synchRespMap = new ConcurrentHashMap<Integer, BaseMessage>();
        creatTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) attributeMap.get(name);
    }

    public long getCreationTime() {
        return creatTime;
    }

    public String getId() {
        return sessionId;
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
        attributeMap.put(name, value);
    }

    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    @Override
    public String toString() {
        return "OMCSession [remoteIp=" + remoteIp + ", session=" + session + ", creatTime=" + creatTime
                + ", maxInactiveInterval=" + maxInactiveInterval + ", sessionId="
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

    public boolean notifySyncMessage(BaseMessage baseMsg) {
        BaseMessage respMsg = (BaseMessage) baseMsg;
        if (isRequestMessage(respMsg.getMessageType())) {
            return false;
        }
        int sequenceId = respMsg.getHead().getSequenceID();
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

    public void sendWithoutResponse(BaseMessage requestMsg) throws Exception {
        assertTransactionSession();
        ByteBuffer buffer = requestMsg.encode();
        session.write(buffer);
    }

    public BaseMessage sendWithResponse(BaseMessage requestMsg, long timeout) throws Exception {
        BaseMessage reqMsg = (BaseMessage) requestMsg;
        assertTransactionSession();

        if (!isRequestMessage(reqMsg.getMessageType())) {
            throw new InvalidParameterException("current message is not a requestMessage, messageType is 0x"
                    + Integer.toHexString(reqMsg.getMessageType()));
        }
        ByteBuffer buffer = reqMsg.encode();// 必须执行encode才可产生sequenceId
        int sequenceId = reqMsg.getHead().getSequenceID();
        synchRespMap.put(sequenceId, reqMsg);
        session.write(buffer);
        if (synchRespMap.get(sequenceId) == reqMsg) {
            synchronized (reqMsg) {
                if (synchRespMap.get(sequenceId) == reqMsg) {
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
    public BaseMessage sendWithResponse(BaseMessage requestMsg) throws Exception {
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
}
