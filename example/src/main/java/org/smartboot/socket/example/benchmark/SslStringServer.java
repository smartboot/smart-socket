/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslStringServer.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.example.StringProtocol;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.BufferPageMonitorPlugin;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class SslStringServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslStringServer.class);

    public static void main(String[] args) throws IOException {
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
                    LOGGER.error(stateMachineEnum + " exception:", throwable);
                }
            }
        };

        AioQuickServer server = new AioQuickServer(8888, new StringProtocol(), processor);
        server.setReadBufferSize(1024 * 1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferFactory(() -> new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors() + 1, true))
                .setWriteBuffer(4096, 512);
        processor.addPlugin(new BufferPageMonitorPlugin<>(server, 6));
        processor.addPlugin(new MonitorPlugin<>(5));
        SslPlugin<String> sslPlugin = new SslPlugin<>();
        sslPlugin.initForServer(StringServer.class.getClassLoader().getResourceAsStream("server.keystore"), "123456", "123456", ClientAuth.NONE);
        processor.addPlugin(sslPlugin);
        server.start();

    }
}
