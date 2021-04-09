/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: NettyCompareDemo.java
 * Date: 2021-03-08
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.netty;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.example.LongProtocol;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/8
 */
public class SmartServer {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "org.smartboot.aio.EnhanceAsynchronousChannelProvider");
        AioQuickServer server = new AioQuickServer("localhost", 8080, new LongProtocol(), (session, msg) -> {
            long now = System.nanoTime();
//                System.out.println("cost: " + (now - msg));
            try {
                session.writeBuffer().writeLong(now);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.setBufferPagePool(new BufferPagePool(1024, Runtime.getRuntime().availableProcessors(), true));
        server.start();
    }


}
