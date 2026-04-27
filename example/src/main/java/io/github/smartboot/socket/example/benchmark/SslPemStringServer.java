/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslPemStringServer.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.benchmark;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.extension.plugins.BufferPageMonitorPlugin;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.extension.plugins.SslPlugin;
import io.github.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;
import io.github.smartboot.socket.transport.AioQuickServer;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class SslPemStringServer {

    public static void main(String[] args) throws Exception {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
//                LOGGER.info(msg);
                WriteBuffer outputStream = session.writeBuffer();

                try {
                    byte[] bytes = msg.getBytes();
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };

        AioQuickServer server = new AioQuickServer(8888, new StringProtocol(), processor);
        server.setReadBufferSize(1024 * 1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferPagePool(new BufferPagePool(Runtime.getRuntime().availableProcessors() + 1, true))
                .setWriteBuffer(4096, 512);
        processor.addPlugin(new BufferPageMonitorPlugin<>(server, 6));
        processor.addPlugin(new MonitorPlugin<>(5));
        SslPlugin<String> sslPlugin = new SslPlugin<>(new PemServerSSLContextFactory(SslPemStringServer.class.getClassLoader().getResourceAsStream("localhost-full.pem")));
        processor.addPlugin(sslPlugin);
        server.start();

    }
}
