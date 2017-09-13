package org.smartboot.socket.transport;

import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {

    @Override
    public void completed(Integer result, Attachment attachment) {
        //读操作回调
        if (attachment.isRead()) {
            attachment.getAioSession().readFromChannel(result);
        } else {
            attachment.getAioSession().tryReleaseFlowLimit();
            attachment.getAioSession().writeToChannel();
        }

    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        attachment.getAioSession().getIoServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}