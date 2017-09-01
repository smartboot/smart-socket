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
        aioSession.readFromChannel();
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        logger.info(exc);
        aioSession.close();
    }
}