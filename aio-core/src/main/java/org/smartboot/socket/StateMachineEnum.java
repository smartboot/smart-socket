/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StateMachineEnum.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * 列举了当前smart-socket所关注的各类状态枚举。
 *
 * <p>当前枚举的各状态机事件在发生后都会及时触发{@link MessageProcessor#stateEvent(AioSession, StateMachineEnum, Throwable)}方法。因此用户在实现的{@linkplain MessageProcessor}接口中可对自己关心的状态机事件进行处理。</p>
 *
 * @author 三刀
 * @version V1.0.0 2018/5/19
 * @see MessageProcessor
 */
public enum StateMachineEnum {
    /**
     * 连接已建立并构建Session对象
     */
    NEW_SESSION,
    /**
     * 读通道已被关闭。
     * <p>
     * 通常由以下几种情况会触发该状态：
     * <ol>
     * <li>对端主动关闭write通道，致使本通常满足了EOF条件</li>
     * <li>当前AioSession处理完读操作后检测到自身正处于{@link StateMachineEnum#SESSION_CLOSING}状态</li>
     * </ol>
     * </p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    INPUT_SHUTDOWN,
    /**
     * 业务处理异常。
     * <p>执行{@link MessageProcessor#process(AioSession, Object)}期间发生用户未捕获的异常。</p>
     */
    PROCESS_EXCEPTION,

    /**
     * 协议解码异常。
     * <p>执行{@link Protocol#decode(ByteBuffer, AioSession)}期间发生未捕获的异常。</p>
     */
    DECODE_EXCEPTION,
    /**
     * 读操作异常。
     *
     * <p>在底层服务执行read操作期间因发生异常情况出发了{@link java.nio.channels.CompletionHandler#failed(Throwable, Object)}。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    INPUT_EXCEPTION,
    /**
     * 写操作异常。
     * <p>在底层服务执行write操作期间因发生异常情况出发了{@link java.nio.channels.CompletionHandler#failed(Throwable, Object)}。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    OUTPUT_EXCEPTION,
    /**
     * 会话正在关闭中。
     *
     * <p>执行了{@link AioSession#close(boolean false)}方法，并且当前还存在待输出的数据。</p>
     */
    SESSION_CLOSING,
    /**
     * 会话关闭成功。
     *
     * <p>AioSession关闭成功</p>
     */
    SESSION_CLOSED,

    /**
     * 拒绝接受连接,仅Server端有效
     */
    REJECT_ACCEPT,

    /**
     * 服务端接受连接异常
     */
    ACCEPT_EXCEPTION,

}
