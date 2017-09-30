package org.smartboot.socket.transport;

/**
 * 会话状态
 *
 * @author 三刀
 * @version V1.0 , 2017/9/5
 */
public enum SessionStatus {
    /**
     * Session状态:已关闭
     */
    SESSION_STATUS_CLOSED,

    /**
     * Session状态:关闭中
     */
    SESSION_STATUS_CLOSING,
    /**
     * Session状态:正常
     */
    SESSION_STATUS_ENABLED;
}
