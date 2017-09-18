package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.transport.AioSession.Attachment;
import org.smartboot.socket.util.StateMachineEnum;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 读写事件回调处理类
 */
class AioCompletionHandler implements CompletionHandler<Integer, Attachment> {
    private static final Logger LOGGER = LogManager.getLogger(AioCompletionHandler.class);
    private boolean asyncRead;

    Thread[] threadArray = new Thread[8];
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(1024);

    public AioCompletionHandler(boolean asyncRead) {
        this.asyncRead = asyncRead;
        if (asyncRead) {
            for (int i = 0; i < threadArray.length; i++) {
                threadArray[i] = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Runnable runnable = queue.take();
                                runnable.run();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                threadArray[i].start();
            }
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
                try {
                    queue.put(new Runnable() {
                        @Override
                        public void run() {
                            attachment.getAioSession().readFromChannel(result);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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