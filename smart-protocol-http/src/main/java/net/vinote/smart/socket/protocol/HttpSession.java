package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.transport.TransportSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Seer
 */
public class HttpSession implements Session<HttpEntity> {
    private Logger logger = LogManager.getLogger(HttpSession.class);
    private String remoteIp;
    private String localAddress;
    private TransportSession<HttpEntity> session;
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

    public HttpSession(TransportSession<HttpEntity> session) {
        sessionId = session.getSessionID();
        remoteIp = session.getRemoteAddr();
        localAddress = session.getLocalAddress();
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

    public String getRemoteIp() {
        return remoteIp;
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
        ByteBuffer buffer=  ByteBuffer.allocate(1024);
        buffer.put(("HTTP/1.1 200 OK\n" +
                "Server: nginx/1.4.4\n" +
                "Date: Mon, 26 Jun 2017 14:37:00 GMT\n" +
                "Content-Type: application/json\n" +
                "Transfer-Encoding: chunked\n" +
                "Connection: keep-alive\n" +
                "X-ASEN: YOU MAKE ME A SAD PANDA.\n" +
                "X-Seraph-LoginReason: OK\n" +
                "X-AUSERNAME: junwei.zheng\n" +
                "Cache-Control: no-cache, no-store, no-transform\n" +
                "X-Content-Type-Options: nosniff\n" +
                "\n" +
                "29\n" +
                "{\"count\":0,\"timeout\":30,\"maxTimeout\":300}\n" +
                "0\n").getBytes());
        session.write(buffer);
        session.close(false);
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
