package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.transport.AioSession.Attachment;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger logger = LogManager.getLogger(AioCompletionHandler.class);

    @Override
    public void completed(Integer result, Attachment attachment) {
        if (attachment.isRead()) {
            if (result == -1) {
                attachment.getAioSession().close(false);
                return;
            }
            attachment.getAioSession().readFromChannel();
        } else {
            attachment.getAioSession().tryReleaseFlowLimit();
            attachment.getAioSession().writeToChannel();
        }

    }

    @Override
    public void failed(Throwable exc, Attachment aioSession) {
        logger.info(exc);
        aioSession.getAioSession().close();
    }
}