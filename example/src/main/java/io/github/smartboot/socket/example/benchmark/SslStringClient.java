/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslStringClient.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.benchmark;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.extension.plugins.SslPlugin;
import io.github.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class SslStringClient {


    public static void main(String[] args) throws Exception {

        BufferPagePool bufferPagePool = new BufferPagePool(10, true);
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };
        processor.addPlugin(new MonitorPlugin(5));
        SslPlugin sslPlugin = new SslPlugin(new ClientSSLContextFactory());
        processor.addPlugin(sslPlugin);
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ClientGroup");
            }
        });
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        new SslStringClient().test(asynchronousChannelGroup, bufferPagePool, processor);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }

    }

    public void test(AsynchronousChannelGroup asynchronousChannelGroup, BufferPagePool bufferPagePool, AbstractMessageProcessor<String> processor) throws InterruptedException, ExecutionException,
            IOException {
        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), processor);
        client.setBufferPagePool(bufferPagePool);
        client.setWriteBuffer(1024 * 1024, 10);
        AioSession session = client.start(asynchronousChannelGroup);
        WriteBuffer outputStream = session.writeBuffer();

        byte[] data = "smart-socket".getBytes();
        while (true) {
            int num = (int) (Math.random() * 10) + 1;
//            int num = 4;
            outputStream.writeInt(data.length * num);
            while (num-- > 0) {
                outputStream.write(data);
            }

//            Thread.sleep(100);
        }
    }
}
