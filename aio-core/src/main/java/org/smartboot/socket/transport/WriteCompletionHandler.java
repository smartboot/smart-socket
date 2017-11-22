package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
class WriteCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LogManager.getLogger(WriteCompletionHandler.class);

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        // 接收到的消息进行预处理
        for (Filter h : aioSession.getServerConfig().getFilters()) {
            h.writeFilter(aioSession, result);
        }
        aioSession.writeToChannel();
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        LOGGER.catching(exc);
        try {
            aioSession.close();
        } catch (Exception e) {
            LOGGER.catching(e);
        }
        aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}