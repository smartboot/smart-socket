package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger LOGGER = LogManager.getLogger(AioCompletionHandler.class);

    @Override
    public void completed(final Integer result, final Attachment attachment) {
        if (attachment.isRead()) {
            // 接收到的消息进行预处理
            for (Filter h : attachment.getServerConfig().getFilters()) {
                h.readFilter(attachment.getAioSession(), result);
            }
            attachment.getAioSession().readFromChannel(result);
        } else {
            // 接收到的消息进行预处理
            for (Filter h : attachment.getServerConfig().getFilters()) {
                h.writeFilter(attachment.getAioSession(), result);
            }
            attachment.getAioSession().writeToChannel();
        }
    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        LOGGER.catching(exc);
        try {
            attachment.getAioSession().close();
        } catch (Exception e) {
            LOGGER.catching(e);
        }
        attachment.getServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}