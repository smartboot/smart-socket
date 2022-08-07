/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpDemo.java
 * Date: 2021-03-01
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.udp;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;
import org.smartboot.socket.transport.UdpChannel;
import org.smartboot.socket.transport.Worker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/1
 */
public class UdpClientDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
//                System.out.println("收到服务端消息: " + msg);
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };
        processor.addPlugin(new MonitorPlugin<>(5, true));

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors(), true);

        Worker worker = new Worker(bufferPagePool, Runtime.getRuntime().availableProcessors());

        int c = 512;
        CountDownLatch count = new CountDownLatch(c);
        byte[] bytes = "hello smart-socket".getBytes();
        for (int i = 0; i < c; i++) {
            new Thread(() -> {
                try {
                    UdpBootstrap bootstrap = new UdpBootstrap(new StringProtocol(), processor, worker);
                    bootstrap.setBannerEnabled(false)
                            .setThreadNum(Runtime.getRuntime().availableProcessors())
                            .setBufferPagePool(bufferPagePool)
                            .setReadBufferSize(1024);
                    UdpChannel channel = bootstrap.open();
                    AioSession session = channel.connect("localhost", 8888);
                    for (int i1 = 0; i1 < 1000000; i1++) {
                        synchronized (session.writeBuffer()) {
                            session.writeBuffer().writeInt(bytes.length);
                            session.writeBuffer().write(bytes);
                            session.writeBuffer().flush();
                        }
                        Thread.sleep(10);
                    }
                    count.countDown();
//                    session.close();
                    System.out.println("发送完毕");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        count.await();
        System.out.println("shutdown...");

    }
}
