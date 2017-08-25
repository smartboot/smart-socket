package org.smartboot.socket.service.filter.impl;

import org.smartboot.socket.service.filter.SmartFilter;
import org.smartboot.socket.service.filter.SmartFilterChain;
import org.smartboot.socket.service.process.MessageProcessor;
import org.smartboot.socket.transport.AioSession;

/**
 * 业务层消息预处理器
 *
 * @author Seer
 * @version SmartFilterChainImpl.java, v 0.1 2015年8月26日 下午5:08:31 Seer Exp.
 */
public class SmartFilterChainImpl<T> implements SmartFilterChain<T> {
    private MessageProcessor<T> receiver;
    private SmartFilter<T>[] handlers = null;
    private boolean withoutFilter = true;//是否无过滤器

    public SmartFilterChainImpl(MessageProcessor<T> receiver, SmartFilter<T>[] handlers) {
        this.receiver = receiver;
        this.handlers = handlers;
        this.withoutFilter = handlers == null || handlers.length == 0;
    }

    public void doChain(AioSession<T> session, T dataEntry, int readSize) {
        if (dataEntry == null) {
            return;
        }
        if (withoutFilter) {
            try {
                receiver.process(session, dataEntry);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // 接收到的消息进行预处理
        for (SmartFilter<T> h : handlers) {
            h.readFilter(session, dataEntry, readSize);
        }
        try {
            for (SmartFilter<T> h : handlers) {
                h.processFilter(session, dataEntry);
            }
            receiver.process(session, dataEntry);
        } catch (Exception e) {
            e.printStackTrace();
            for (SmartFilter<T> h : handlers) {
                h.processFailHandler(session, dataEntry);
            }
        }
    }
}
