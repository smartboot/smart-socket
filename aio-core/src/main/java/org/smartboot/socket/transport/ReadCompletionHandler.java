/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReadCompletionHandler.java
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
class ReadCompletionHandler<T> implements CompletionHandler<Integer, TcpAioSession<T>> {
    /**
     * 处理消息读回调事件
     *
     * @param result     已读消息字节数
     * @param aioSession 当前触发读回调的会话
     */
    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        try {
            // 接收到的消息进行预处理
            NetMonitor monitor = aioSession.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.afterRead(aioSession, result);
            }
            //触发读回调
            aioSession.flipRead(result == -1);
            aioSession.signalRead();
        } catch (Exception e) {
            failed(e, aioSession);
        }
    }


    @Override
    public final void failed(Throwable exc, TcpAioSession<T> aioSession) {
        try {
            aioSession.getServerConfig().getProcessor().stateEvent(aioSession, StateMachineEnum.INPUT_EXCEPTION, exc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //兼容性处理，windows要强制关闭,其他系统优雅关闭
            //aioSession.close(IOUtil.OS_WINDOWS);
            aioSession.close(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {

    }

}