package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.io.Channel;
import net.vinote.smart.socket.service.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Seer
 */
public class HttpSession implements Session<HttpEntity> {
    private Channel<HttpEntity> session;
    /**
     * 会话创建时间
     */
    private final long creatTime;
    /**
     * 上一次访问时间
     */
    private long lastAccessTime;
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
    private Map<Integer, HttpEntity> synchRespMap;

    private Map<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

    public HttpSession(Channel<HttpEntity> session) {
        sessionId = session.getSessionID();
        this.session = session;
        maxInactiveInterval = session.getTimeout();
        synchRespMap = new ConcurrentHashMap<Integer, HttpEntity>();
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


    public void invalidate(boolean immediate) {

        invalidated = true;
        for (HttpEntity unit : synchRespMap.values()) {
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
        return "OMCSession [ session=" + session + ", creatTime=" + creatTime
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


	/*
     * (non-Javadoc)
	 * 
	 * @see
	 * com.zjw.platform.quickly.service.session.Session#notifySyncMessage(com
	 * .zjw.platform.quickly.protocol.DataEntry)
	 */

    public boolean notifySyncMessage(HttpEntity baseMsg) {
        return false;

    }

	/*
     * (non-Javadoc)
	 * 
	 * @see
	 * com.zjw.platform.quickly.service.session.Session#sendWithoutResponse(
	 * com.zjw.platform.quickly.protocol.DataEntry)
	 */

    public void sendWithoutResponse(HttpEntity requestMsg) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(("HTTP/1.1 200 OK\n" +
                "Server: seer/1.4.4\n" +
                "Content-Length: 2\n" +
                "Connection: keep-alive\n" +
                "\n" +
                "OK").getBytes());
        session.write(buffer);
        if (!"Keep-Alive".equalsIgnoreCase(requestMsg.getHeadMap().get("Connection"))) {
            session.close(false);
        }
//        throw new UnsupportedOperationException();
    }

    public HttpEntity sendWithResponse(HttpEntity requestMsg, long timeout) throws Exception {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.vinote.smart.socket.service.session.Session#sendWithResponse(net.
     * vinote.smart.socket.protocol.DataEntry)
     */
    public HttpEntity sendWithResponse(HttpEntity requestMsg) throws Exception {
        return sendWithResponse(requestMsg, session.getTimeout());
    }


}
