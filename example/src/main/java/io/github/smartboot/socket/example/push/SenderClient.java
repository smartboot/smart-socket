/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SenderClient.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.push;

import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class SenderClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        StringProtocol protocol = new StringProtocol();
        AioQuickClient clients = new AioQuickClient("localhost", 8080, protocol, (session, msg) -> System.out.println("ReceiverClient:" + session + " 收到Push消息:" + msg));
        AioSession session = clients.start();
        byte[] msg = "HelloWorld".getBytes();
        while (true) {
            WriteBuffer writeBuffer = session.writeBuffer();
            writeBuffer.writeInt(msg.length);
            writeBuffer.write(msg);
            writeBuffer.flush();
            Thread.sleep(1000);
        }
    }
}
