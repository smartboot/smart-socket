/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MessageDispatcher.java
 * Date: 2020-03-13
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * UDP消息分发器
 *
 * @author 三刀
 * @version V1.0 , 2020/3/13
 */
class UdpDispatcher<T> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpDispatcher.class);
    public final RequestTask EXECUTE_TASK_OR_SHUTDOWN = new RequestTask(null, null);
    private final BlockingQueue<RequestTask> taskQueue = new LinkedBlockingQueue<>();
    private final MessageProcessor<T> processor;

    public UdpDispatcher(MessageProcessor<T> processor) {
        this.processor = processor;
    }

    @Override
    public void run() {
        while (true) {
            try {
                RequestTask unit = taskQueue.take();
                if (unit == EXECUTE_TASK_OR_SHUTDOWN) {
                    LOGGER.info("shutdown thread:{}", Thread.currentThread());
                    break;
                }
                processor.process(unit.session, unit.request);
                unit.session.writeBuffer().flush();
            } catch (InterruptedException e) {
                LOGGER.info("InterruptedException", e);
            } catch (Exception e) {
                LOGGER.error(e.getClass().getName(), e);
            }
        }
    }

    /**
     * 任务分发
     *
     * @param session
     * @param request
     */
    public void dispatch(UdpAioSession session, T request) {
        dispatch(new RequestTask(session, request));
    }

    /**
     * 任务分发
     *
     * @param requestTask
     */
    public void dispatch(RequestTask requestTask) {
        taskQueue.offer(requestTask);
    }

    class RequestTask {
        UdpAioSession session;
        T request;

        public RequestTask(UdpAioSession session, T request) {
            this.session = session;
            this.request = request;
        }
    }
}
