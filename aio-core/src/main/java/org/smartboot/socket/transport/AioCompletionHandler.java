package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger LOGGER = LogManager.getLogger(AioCompletionHandler.class);
    private ExecutorService readService = null;
    private boolean asyncRead;

    public AioCompletionHandler(boolean asyncRead) {
        this.asyncRead = asyncRead;
        if (asyncRead) {
            readService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE,
                    10L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1024));
        }
    }

    @Override
    public void completed(final Integer result, final Attachment attachment) {
        //出现result为0,说明代码存在问题
        if (result == 0) {
            LOGGER.error("result is 0");
        }
        //读操作回调
        if (attachment.isRead()) {
            if (asyncRead) {
                readService.execute(new Runnable() {
                    @Override
                    public void run() {
                        attachment.getAioSession().readFromChannel(result);
                    }
                });
            } else {
                attachment.getAioSession().readFromChannel(result);
            }
        } else {
            attachment.getAioSession().writeToChannel();
        }

    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        attachment.getAioSession().getIoServerConfig().getProcessor().stateEvent(attachment.getAioSession(), attachment.isRead() ? StateMachineEnum.INPUT_EXCEPTION : StateMachineEnum.OUTPUT_EXCEPTION, exc);
    }
}