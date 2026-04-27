/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerDemo.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.basic;

import io.github.smartboot.socket.MessageProcessor;
import io.github.smartboot.socket.example.IntegerProtocol;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioQuickServer;
import io.github.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerDemo {
    public static void main(String[] args) throws Exception {
        //启动服务端
        MessageProcessor<Integer> serverProcessor = (session, msg) -> {
            int respMsg = msg + 1;
            System.out.println("receive data from client: " + msg + " ,rsp:" + (respMsg));
            try {
                session.writeBuffer().writeInt(respMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        AioQuickServer server = new AioQuickServer(8888, new IntegerProtocol(), serverProcessor);
        server.start();

        //启动客户端
        MessageProcessor<Integer> clientProcessor = (session, msg) -> System.out.println("receive data from server：" + msg);
        AioQuickClient aioQuickClient = new AioQuickClient("localhost", 8888, new IntegerProtocol(), clientProcessor);
        AioSession session = aioQuickClient.start();
        //encode
        session.writeBuffer().writeInt(1);
        //flush data
        session.writeBuffer().flush();

        //shutdown
        Thread.sleep(1000);
        aioQuickClient.shutdownNow();
        server.shutdown();
    }
}
