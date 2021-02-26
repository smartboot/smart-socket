/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AbstractMessageProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.processor;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class AbstractMessageProcessor<T> implements MessageProcessor<T>, NetMonitor {

    private final List<Plugin<T>> plugins = new ArrayList<>();

    @Override
    public final void afterRead(AioSession session, int readSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.afterRead(session, readSize);
        }
    }

    @Override
    public final void afterWrite(AioSession session, int writeSize) {
        for (Plugin<T> plugin : plugins) {
            plugin.afterWrite(session, writeSize);
        }
    }

    @Override
    public final void beforeRead(AioSession session) {
        for (Plugin<T> plugin : plugins) {
            plugin.beforeRead(session);
        }
    }

    @Override
    public final void beforeWrite(AioSession session) {
        for (Plugin<T> plugin : plugins) {
            plugin.beforeWrite(session);
        }
    }

    @Override
    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        AsynchronousSocketChannel acceptChannel = channel;
        for (Plugin<T> plugin : plugins) {
            acceptChannel = plugin.shouldAccept(acceptChannel);
            if (acceptChannel == null) {
                return null;
            }
        }
        return acceptChannel;
    }

    @Override
    public final void process(AioSession session, T msg) {
        boolean flag = true;
        for (Plugin<T> plugin : plugins) {
            if (!plugin.preProcess(session, msg)) {
                flag = false;
            }
        }
        if (flag) {
            process0(session, msg);
        }
    }

    /**
     * 处理接收到的消息
     *
     * @param session
     * @param msg
     * @see MessageProcessor#process(AioSession, Object)
     */
    public abstract void process0(AioSession session, T msg);

    /**
     * @param session          本次触发状态机的AioSession对象
     * @param stateMachineEnum 状态枚举
     * @param throwable        异常对象，如果存在的话
     */
    @Override
    public final void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        for (Plugin<T> plugin : plugins) {
            plugin.stateEvent(stateMachineEnum, session, throwable);
        }
        stateEvent0(session, stateMachineEnum, throwable);
    }

    /**
     * @param session
     * @param stateMachineEnum
     * @param throwable
     * @see #stateEvent(AioSession, StateMachineEnum, Throwable)
     */
    public abstract void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable);

    public final void addPlugin(Plugin<T> plugin) {
        this.plugins.add(plugin);
    }
}
