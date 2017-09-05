package org.smartboot.socket.transport;

/**
 * 会话状态
 * @author Seer
 * @version V1.0 , 2017/9/5
 */
public class SessionStatus {
    /**
     * Session状态:已关闭
     */
    public static final byte SESSION_STATUS_CLOSED = 1;

    /**
     * Session状态:关闭中
     */
    public static final byte SESSION_STATUS_CLOSING = 2;
    /**
     * Session状态:正常
     */
    public static final byte SESSION_STATUS_ENABLED = 3;
}
