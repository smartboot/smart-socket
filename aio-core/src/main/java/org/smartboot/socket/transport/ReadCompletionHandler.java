package org.smartboot.socket.transport;

import org.smartboot.socket.Filter;
import org.smartboot.socket.util.StateMachineEnum;

/**
 * 读写事件回调处理类
 */
class ReadCompletionHandler<T> extends AbstractCompletionHandler<T> {

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        // 接收到的消息进行预处理
        for (Filter h : aioSession.getServerConfig().getFilters()) {
            h.readFilter(aioSession, result);
        }
        aioSession.readFromChannel(result == -1);
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        super.failed(exc, aioSession);
        aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.INPUT_EXCEPTION, exc);
    }
}