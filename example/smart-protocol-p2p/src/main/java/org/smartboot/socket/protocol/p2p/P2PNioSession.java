package org.smartboot.socket.protocol.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.ioc.transport.NioSession;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.MessageType;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author三刀
 */
public class P2PNioSession {
    /**
     * 会话创建时间
     */
    private final long creatTime;
    private Logger logger = LoggerFactory.getLogger(P2PSession.class);
    private NioSession<BaseMessage> session;
    /**
     * 上一次访问时间
     */
    private long lastAccessTime;
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
    private int timeout;

    public P2PNioSession(NioSession<BaseMessage> session, int timeout) {
        sessionId = String.valueOf(System.identityHashCode(this));
        this.session = session;
        this.timeout = timeout;
        synchRespMap = new ConcurrentHashMap<Integer, BaseMessage>();
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


    public boolean isInvalid() {
        return invalidated;
    }

    private void assertTransactionSession() throws IOException {
        if (session == null || session.isInvalid()) {
            throw new IOException("Socket Channel is invalid now");
        }
    }

    public void refreshAccessedTime() {
        lastAccessTime = System.currentTimeMillis();
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
        refreshAccessedTime();
        ByteBuffer buffer = requestMsg.encode();
        session.write(buffer);
    }

    public BaseMessage sendWithResponse(BaseMessage requestMsg, long timeout) throws Exception {
        BaseMessage reqMsg = (BaseMessage) requestMsg;
        assertTransactionSession();
        refreshAccessedTime();

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
        return sendWithResponse(requestMsg, timeout);
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
