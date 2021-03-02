/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SenderClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderClient.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        StringProtocol protocol = new StringProtocol();
        AioQuickClient clients = new AioQuickClient("localhost", 8080, protocol, (session, msg) -> LOGGER.info("ReceiverClient:{} 收到Push消息:{}", session.getSessionID(), msg));
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
