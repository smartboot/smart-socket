/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ClosedClient.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.benchmark;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/19
 */
public class ClosedClient {
    public static void main(String[] args) throws IOException {
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {
            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
            }
        });
        byte[] data = "smart-socket".getBytes();
        int i = 10000;
        while (i-- > 0) {
            AioSession session = client.start();
            service.execute(() -> {
                try {
                    session.writeBuffer().writeInt(data.length);
                    session.writeBuffer().write(data);
                    session.writeBuffer().flush();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            });
            client.shutdownNow();
        }
        service.shutdown();
    }
}
