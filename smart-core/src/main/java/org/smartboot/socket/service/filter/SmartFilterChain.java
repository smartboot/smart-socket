package org.smartboot.socket.service.filter;

import org.smartboot.socket.transport.AioSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 */
public interface SmartFilterChain<T> {

    void doChain(AioSession<T> session, T buffer, int readSize);
}
