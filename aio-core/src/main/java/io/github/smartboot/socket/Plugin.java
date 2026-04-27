/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Plugin.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket;

import io.github.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public interface Plugin<T> {

    /**
     * <p>
     * 监控已接收到的连接
     * </p>
     *
     * @param channel 当前已经建立连接的通道对象
     * @return 非null:接受该连接,null:拒绝该连接
     */
    default AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return channel;
    }

    /**
     * 监控触发本次读回调Session的已读数据字节数
     *
     * @param session  当前执行read的AioSession对象
     * @param readSize 已读数据长度
     */
    default void afterRead(AioSession session, int readSize) {
    }

    /**
     * 即将开始读取数据
     *
     * @param session 当前会话对象
     */
    default void beforeRead(AioSession session) {
    }

    /**
     * 监控触发本次写回调session的已写数据字节数
     *
     * @param session   本次执行write回调的AIOSession对象
     * @param writeSize 本次输出的数据长度
     */
    default void afterWrite(AioSession session, int writeSize) {
    }

    /**
     * 即将开始写数据
     *
     * @param session 当前会话对象
     */
    default void beforeWrite(AioSession session) {
    }

    /**
     * 对请求消息进行预处理，并决策是否进行后续的MessageProcessor处理。
     * 若返回false，则当前消息将被忽略。
     * 若返回true，该消息会正常秩序MessageProcessor.process.
     *
     * @param session
     * @param t
     * @return
     */
    default boolean preProcess(AioSession session, T t) {
        return true;
    }


    /**
     * 监听状态机事件
     *
     * @param stateMachineEnum
     * @param session
     * @param throwable
     * @see MessageProcessor#stateEvent(AioSession, StateMachineEnum, Throwable)
     */
    default void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
    }

}
