/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteCompletionHandler.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * 读写事件回调处理类
 *
 * @author 三刀
 * @version V1.0.0
 */
final class WriteCompletionHandler implements CompletionHandler<Integer, TcpAioSession> {

    @Override
    public void completed(final Integer result, final TcpAioSession aioSession) {
        try {
            NetMonitor monitor = aioSession.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.afterWrite(aioSession, result);
            }
            aioSession.writeCompleted();
        } catch (Exception e) {
            failed(e, aioSession);
        }
    }


    @Override
    public void failed(Throwable exc, TcpAioSession aioSession) {
        try {
            aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.OUTPUT_EXCEPTION, exc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            aioSession.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}