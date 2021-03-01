/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SenderClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

import org.smartboot.socket.example.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class SenderClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        StringProtocol protocol = new StringProtocol();
        PushClientProcessorMessage clientProcessorMessage = new PushClientProcessorMessage();
        AioQuickClient clients = new AioQuickClient("localhost", 8080, protocol, clientProcessorMessage);
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
