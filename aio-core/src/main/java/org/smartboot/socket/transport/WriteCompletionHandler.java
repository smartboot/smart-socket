/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteCompletionHandler.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class WriteCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteCompletionHandler.class);

    private ExecutorService executorService;
    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public WriteCompletionHandler() {
    }

    public WriteCompletionHandler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
        if (executorService != null && threadLocal.get() == null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    threadLocal.set("");
                    completed0(result, aioSession);
                    threadLocal.remove();
                }
            });
        } else {
            completed0(result, aioSession);
        }

    }

    private void completed0(final Integer result, final AioSession<T> aioSession) {
        try {
            // 接收到的消息进行预处理
            NetMonitor<T> monitor = aioSession.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.writeMonitor(aioSession, result);
            }
            aioSession.writeToChannel();
        } catch (Exception e) {
            failed(e, aioSession);
        }
    }

    @Override
    public void failed(Throwable exc, AioSession<T> aioSession) {
        try {
            aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.OUTPUT_EXCEPTION, exc);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            aioSession.close();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }
}