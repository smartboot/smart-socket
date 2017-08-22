package org.smartboot.socket.service.filter.impl;

import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.filter.SmartFilterChain;
import org.smartboot.socket.service.process.ProtocolDataProcessor;
import org.smartboot.socket.transport.AioSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 * @version SmartFilterChainImpl.java, v 0.1 2015年8月26日 下午5:08:31 Seer Exp.
 */
public class SmartFilterChainImpl<T> implements SmartFilterChain<T> {
    private ProtocolDataProcessor<T> receiver;
    private SmartFilter<T>[] handlers = null;
    private boolean hasHandlers = false;

    public SmartFilterChainImpl(ProtocolDataProcessor<T> receiver, SmartFilter<T>[] handlers) {
        this.receiver = receiver;
        this.handlers = handlers;
        this.hasHandlers = (handlers != null && handlers.length > 0);
    }

    public void doChain(AioSession<T> session, T dataEntry, int readSize) {
        if (dataEntry == null) {
            return;
        }
        // 接收到的消息进行预处理
        if (hasHandlers) {
            for (SmartFilter<T> h : handlers) {
                h.readFilter(session, dataEntry,readSize);
            }
        }
        boolean succ = true;
        try {
            if (hasHandlers) {
                for (SmartFilter<T> h : handlers) {
                    h.processFilter(session, dataEntry);
                }
            }
            receiver.process(session, dataEntry);
        } catch (Exception e) {
            e.printStackTrace();
            succ = false;
        }
        // 未能成功接受消息
        if (!succ && hasHandlers) {
            for (SmartFilter<T> h : handlers) {
                h.processFailHandler(session, dataEntry);
            }
        }

    }
}
