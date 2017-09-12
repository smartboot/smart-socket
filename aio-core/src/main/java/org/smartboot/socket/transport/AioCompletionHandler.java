package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger logger = LogManager.getLogger(AioCompletionHandler.class);

    @Override
    public void completed(Integer result, Attachment attachment) {
        //都操作回调
        if (attachment.isRead()) {
            attachment.getAioSession().readFromChannel(result);
        } else {
            attachment.getAioSession().tryReleaseFlowLimit();
            attachment.getAioSession().writeToChannel();
        }

    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        logger.info(exc);
        attachment.getAioSession().getIoServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION);
    }
}