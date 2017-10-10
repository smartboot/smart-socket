package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.service.SmartFilter;
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
        //出现result为0,说明代码存在问题
        if (result == 0) {
            LOGGER.error("result is 0");
        }
        //读操作回调
        if (attachment.isRead()) {
            if (result == -1) {
                attachment.getServerConfig().getProcessor().stateEvent(attachment.getAioSession(), StateMachineEnum.INPUT_SHUTDOWN, null);
                return;
            }
            // 接收到的消息进行预处理
            for (SmartFilter h : attachment.getServerConfig().getFilters()) {
                h.readFilter(attachment.getAioSession(), result);
            }
            attachment.getAioSession().readFromChannel();
        } else {
            // 接收到的消息进行预处理
            for (SmartFilter h : attachment.getServerConfig().getFilters()) {
                h.writeFilter(attachment.getAioSession(), result);
            }
            attachment.getAioSession().tryReleaseFlowLimit();
            attachment.getAioSession().writeToChannel();
        }
    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        LOGGER.catching(exc);
        attachment.getServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}