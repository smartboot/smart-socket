/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Plugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public interface Plugin<T> extends NetMonitor {

    /**
     * 对请求消息进行预处理，并决策是否进行后续的MessageProcessor处理。
     * 若返回false，则当前消息将被忽略。
     * 若返回true，该消息会正常秩序MessageProcessor.process.
     *
     * @param session
     * @param t
     * @return
     */
    boolean preProcess(AioSession session, T t);


    /**
     * 监听状态机事件
     *
     * @param stateMachineEnum
     * @param session
     * @param throwable
     * @see org.smartboot.socket.MessageProcessor#stateEvent(AioSession, StateMachineEnum, Throwable)
     */
    void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable);

}
