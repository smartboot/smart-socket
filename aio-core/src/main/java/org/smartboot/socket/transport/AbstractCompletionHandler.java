package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
abstract class AbstractCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LogManager.getLogger(AbstractCompletionHandler.class);

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        LOGGER.catching(exc);
        try {
            aioSession.close();
        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }
}