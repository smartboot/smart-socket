package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.CompletionHandler;

class ReadCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger logger = LogManager.getLogger(ReadCompletionHandler.class);


    @Override
    public void completed(Integer result, AioSession<T> aioSession) {
        if (result == -1) {
            logger.debug("read end:" + aioSession);
            aioSession.close(false);
            return;
        }

        aioSession.decodeAndProcess();

        //会话已不可用,终止读
        if (aioSession.isInvalid()) {
            return;
        }

        //是否触发流控
        if (aioSession.serverFlowLimit != null && aioSession.writeCacheQueue.size() > aioSession.FLOW_LIMIT_LINE) {
            aioSession.serverFlowLimit.set(true);
        } else {
            aioSession.registerReadHandler();
        }
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        logger.info(exc);
        aioSession.close();
    }
}