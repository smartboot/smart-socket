/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RateLimiterDemo.java
 * Date: 2021-10-21
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.example.benchmark.StringServer;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.plugins.RateLimiterPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/10/21
 */
public class RateLimiterDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringServer.class);

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
                    LOGGER.error(stateMachineEnum + " exception:", throwable);
                }
            }
        };

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors() + 1, true);
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
