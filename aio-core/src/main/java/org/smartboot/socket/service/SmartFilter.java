package org.smartboot.socket.service;

import org.smartboot.socket.transport.AioSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 */
public interface SmartFilter<T> {

    /**
     * 消息处理前置预处理
     *
     * @param session
     * @param d
     */
    public void processFilter(AioSession<T> session, T d);

    /**
     * 消息接受前置预处理
     *
     * @param session
     * @param d
     */
    public void readFilter(AioSession<T> session, T d, int readSize);

    /**
     * 消息接受失败处理
     */
    public void processFailHandler(AioSession<T> session, T d,Exception e);

}
