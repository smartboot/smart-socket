/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpDemo.java
 * Date: 2021-03-01
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/1
 */
public class UdpServerDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpServerDemo.class);

    public static void main(String[] args) throws IOException {
        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
//                System.out.println("收到客户端消息: " + msg);
                byte[] bytes = msg.getBytes();
                try {
                    session.writeBuffer().writeInt(bytes.length);
                    session.writeBuffer().write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                switch (stateMachineEnum) {
                    case SESSION_CLOSING:
                        LOGGER.info("session:{} closing", session);
                }
            }
        };
        processor.addPlugin(new MonitorPlugin<>(5,true));
        UdpBootstrap bootstrap = new UdpBootstrap(new StringProtocol(), processor);
        bootstrap.setThreadNum(Runtime.getRuntime().availableProcessors())
                .setBufferPagePool(new BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors(), true))
                .setReadBufferSize(1024).open(8888);
    }
}
