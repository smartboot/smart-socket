/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Server.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.benchmark;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.transport.AioQuickServer;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * ulimit -n 1000000
 * sysctl -w kern.maxfilesperproc=1000000
 *
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class Server {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("PORT", "8080"));
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

        BufferPagePool bufferPagePool = new BufferPagePool(Runtime.getRuntime().availableProcessors() + 1, true);
        AioQuickServer server = new AioQuickServer(port, new StringProtocol(), processor);
        server.setReadBufferSize(1024)
                .setThreadNum(Runtime.getRuntime().availableProcessors() + 1)
                .setBufferPagePool(bufferPagePool)
                .retainReadBuffer()
                .setWriteBuffer(4096, 1);
        processor.addPlugin(new MonitorPlugin<>(5));
        server.start();

    }
}
