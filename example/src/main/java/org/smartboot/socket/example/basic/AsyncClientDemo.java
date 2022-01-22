/*******************************************************************************
 * Copyright (c) 2017-2022, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AsyncClientDemo.java
 * Date: 2022-01-05
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.basic;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

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
