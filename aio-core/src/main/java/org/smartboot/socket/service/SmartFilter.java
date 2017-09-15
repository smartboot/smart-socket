package org.smartboot.socket.service;

import org.smartboot.socket.transport.AioSession;

/**
 * 消息过滤器
 *
 * @author Seer
 */
public interface SmartFilter<T> {

    /**
     * 消息处理前置预处理
     *
     * @param session
     * @param msgEntity 编解码后的消息实体
     */
    public void processFilter(AioSession<T> session, T msgEntity);

    /**
     * 消息接受前置预处理
     *
     * @param session
     * @param msgEntity 编解码后的消息实体
     * @param readSize  本次解码读取的数据长度
     */
    public void readFilter(AioSession<T> session, T msgEntity, int readSize);

    /**
     * 消息接受失败处理
     *
     * @param session
     * @param msgEntity 编解码后的消息实体
     * @param e         本次处理异常对象
     */
    public void processFailHandler(AioSession<T> session, T msgEntity, Exception e);

}
