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
import org.smartboot.socket.example.LongProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/8
 */
public class SmartClient {
    public static void main(String[] args) throws Exception {
        AtomicLong atomicLong = new AtomicLong(0);
        AtomicLong count = new AtomicLong(0);
        AioQuickClient client = new AioQuickClient("localhost", 8080, new LongProtocol(), new MessageProcessor<Long>() {
            @Override
            public void process(AioSession session, Long msg) {
                long now = System.nanoTime();
                atomicLong.addAndGet(now - msg);
                if (count.incrementAndGet() < 500000) {
                    try {
                        session.writeBuffer().writeLong(now);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(atomicLong.get() * 1.0 / count.get());
                    System.exit(1);
                }
            }
        });
        AioSession aioSession = client.start();
        long start = System.nanoTime();
        aioSession.writeBuffer().writeLong(System.nanoTime());
        aioSession.writeBuffer().flush();
    }


}
