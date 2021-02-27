/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReceiverClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

import org.smartboot.socket.example.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class ReceiverClient {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        StringProtocol protocol = new StringProtocol();
        PushClientProcessorMessage clientProcessorMessage = new PushClientProcessorMessage();
        AioQuickClient[] clients = new AioQuickClient[4];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new AioQuickClient("localhost", 8080, protocol, clientProcessorMessage);
            clients[i].start(channelGroup);
        }
    }
}
