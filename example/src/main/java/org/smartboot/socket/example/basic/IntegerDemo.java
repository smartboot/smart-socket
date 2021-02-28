/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerClient.java
 * Date: 2021-02-28
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.basic;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.example.IntegerProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

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
