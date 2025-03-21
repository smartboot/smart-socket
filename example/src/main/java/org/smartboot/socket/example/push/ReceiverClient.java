/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReceiverClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class ReceiverClient {

    public static void main(String[] args) throws IOException {
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), Thread::new);
        StringProtocol protocol = new StringProtocol();
        AioQuickClient[] clients = new AioQuickClient[4];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new AioQuickClient("localhost", 8080, protocol, (session, msg) -> System.out.println("ReceiverClient:" + session.getSessionID() + " 收到Push消息:" + msg));
            clients[i].start(channelGroup);
        }
    }
}
