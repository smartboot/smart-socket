/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Clients.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.benchmark;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/9/17
 */
public class Clients {
    public static void main(String[] args) throws IOException {
        int count = Integer.parseInt(System.getProperty("count", "10000"));
        String host = System.getProperty("host", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("port", "8080"));
        AsynchronousChannelProvider provider = new EnhanceAsynchronousChannelProvider(true);
        AsynchronousChannelGroup[] groups = new AsynchronousChannelGroup[1];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = provider.openAsynchronousChannelGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r);
                }
            });
        }
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        };
        processor.addPlugin(new MonitorPlugin<>(5));
        BufferPagePool bufferPagePool = new BufferPagePool(Runtime.getRuntime().availableProcessors() + 1, true);
        for (int i = 0; i < count; i++) {
            AioQuickClient client = new AioQuickClient(host, port, new StringProtocol(), processor);
            client.setReadBufferSize(1024).setBufferPagePool(bufferPagePool);
            try {
                client.start(groups[i % groups.length]);
                synchronized (client) {
                    client.wait(1);
                }
            } catch (Throwable throwable) {
                count--;
                throwable.printStackTrace();
                client.shutdownNow();
            }
        }
    }
}
