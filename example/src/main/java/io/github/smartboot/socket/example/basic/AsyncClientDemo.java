/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AsyncClientDemo.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.basic;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/5
 */
public class AsyncClientDemo {
    public static void main(String[] args) throws IOException {
        AioQuickClient client = new AioQuickClient("www1.sdfsdf.com", 1232, new StringProtocol(), new AbstractMessageProcessor<String>() {
            @Override
            public void process0(AioSession session, String msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {

            }
        });
        client.start(null, new CompletionHandler<AioSession, Object>() {
            @Override
            public void completed(AioSession result, Object attachment) {
                System.out.println("success");
                client.shutdown();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println("fail");
                exc.printStackTrace();
            }
        });
    }
}
