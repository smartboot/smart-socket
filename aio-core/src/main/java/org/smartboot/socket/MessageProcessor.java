/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MessageProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

/**
 * 消息处理器。
 *
 * <p>
 * 通过实现该接口，对完成解码的消息进行业务处理。
 * </p>
 *
 * @param <T> 消息对象实体类型
 * @author 三刀
 * @version V1.0.0 2018/5/19
 */
public interface MessageProcessor<T> {

    /**
     * 处理接收到的消息
     *
     * @param session 通信会话
     * @param msg     待处理的业务消息
     */
    void process(AioSession session, T msg);

    /**
     * 状态机事件,当枚举事件发生时由框架触发该方法
     *
     * @param session          本次触发状态机的AioSession对象
     * @param stateMachineEnum 状态枚举
     * @param throwable        异常对象，如果存在的话
     * @see StateMachineEnum
     */
    default void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        if (stateMachineEnum == StateMachineEnum.DECODE_EXCEPTION || stateMachineEnum == StateMachineEnum.PROCESS_EXCEPTION) {
            throwable.printStackTrace();
        }
    }
}
