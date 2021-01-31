/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IOUtil.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import org.smartboot.socket.AsyncSupportMessageProcessor;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.ProcessMode;
import org.smartboot.socket.StateMachineEnum;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/2
 */
final class IOUtil {
    /**
     * @param channel 需要被关闭的通道
     */
    public static void close(AsynchronousSocketChannel channel) {
        if (channel == null) {
            throw new NullPointerException();
        }
        try {
            channel.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> AsyncSupportMessageProcessor<T> wrap(MessageProcessor<T> messageProcessor) {
        return new AsyncSupportMessageProcessor<T>() {
            @Override
            public ProcessMode process(AioSession session, T msg) {
                messageProcessor.process(session, msg);
                return ProcessMode.SYNC;
            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                messageProcessor.stateEvent(session, stateMachineEnum, throwable);
            }
        };
    }
}
