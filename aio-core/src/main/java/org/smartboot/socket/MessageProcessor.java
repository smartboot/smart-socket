/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MessageProcessor.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

/**
 * 消息处理器
 *
 * @author 三刀
 * @version MessageProcessor.java, v 0.1 2015年3月13日 下午3:26:55 Seer Exp.
 * @version V1.0.0
 */
public interface MessageProcessor<T> {

    /**
     * 处理接收到的消息
     *
     * @param session 通信会话
     * @param msg     待处理的业务消息
     * @throws Exception
     */
    public void process(AioSession<T> session, T msg);

    /**
     * 状态机事件,当枚举事件发生时由框架触发该方法
     *
     * @param session
     * @param stateMachineEnum 状态枚举
     * @param throwable        异常对象，如果存在的话
     */
    void stateEvent(AioSession<T> session, StateMachineEnum stateMachineEnum, Throwable throwable);
}
