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

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
class WriteCompletionHandler<T> implements CompletionHandler<Integer, AioSession<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteCompletionHandler.class);

    @Override
    public void completed(final Integer result, final AioSession<T> aioSession) {
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