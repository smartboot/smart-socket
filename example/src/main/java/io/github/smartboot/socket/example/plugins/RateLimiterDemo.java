/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RateLimiterDemo.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.plugins;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.extension.plugins.RateLimiterPlugin;
import io.github.smartboot.socket.transport.AioQuickServer;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/10/21
 */
public class RateLimiterDemo {

    public static void main(String[] args) throws IOException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
                WriteBuffer outputStream = session.writeBuffer();

                try {
                    byte[] bytes = msg.getBytes();
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };

        BufferPagePool bufferPagePool = new BufferPagePool(Runtime.getRuntime().availableProcessors() + 1, true);
        AioQuickServer server = new AioQuickServer(8888, new StringProtocol(), processor);
        server.setReadBufferSize(1024 * 1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferPagePool(bufferPagePool)
                .setWriteBuffer(4096, 512);
        processor.addPlugin(new MonitorPlugin<>(10));
        processor.addPlugin(new RateLimiterPlugin<>(512, 1024));
        server.start();

    }
}
