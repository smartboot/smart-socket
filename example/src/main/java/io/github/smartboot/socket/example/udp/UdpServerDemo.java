/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpServerDemo.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.udp;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.extension.plugins.MonitorPlugin;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.UdpBootstrap;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/1
 */
public class UdpServerDemo {

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
                        System.out.println("session:" + session + " closing");
                }
            }
        };
        processor.addPlugin(new MonitorPlugin<>(5, true));
        UdpBootstrap bootstrap = new UdpBootstrap(new StringProtocol(), processor);
        bootstrap.setThreadNum(Runtime.getRuntime().availableProcessors())
                .setBufferPagePool(new BufferPagePool(Runtime.getRuntime().availableProcessors(), true))
                .setReadBufferSize(1024).open(8888);
    }
}
