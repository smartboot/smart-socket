/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AbstractPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public abstract class AbstractPlugin<T> implements Plugin<T> {
    @Override
    public boolean preProcess(AioSession session, T t) {
        return true;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {

    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return channel;
    }

    @Override
    public void afterRead(AioSession session, int readSize) {

    }

    @Override
    public void afterWrite(AioSession session, int writeSize) {

    }

    @Override
    public void beforeRead(AioSession session) {

    }

    @Override
    public void beforeWrite(AioSession session) {

    }
}
