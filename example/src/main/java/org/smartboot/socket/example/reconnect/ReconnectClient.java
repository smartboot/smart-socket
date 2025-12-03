/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReconnectClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.reconnect;

import org.smartboot.socket.example.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class ReconnectClient {
    private AioQuickClient client;
    private AioSession session;
    private boolean running = true;

    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("启动连接监测");
                while (running) {
                    if (session == null || session.isInvalid()) {
                        System.out.println("连接异常，准备重连...");
                        connect();
                    } else {
                        System.out.println("连接正常...");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("终止连接监测");
            }
        }, "Reconnect-Thread").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("模拟连接异常");
                int i = 0;
                while (i++ < 3) {
                    System.out.println("第 " + i + " 次断开连接");
                    if (session != null) {
                        session.close();
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("彻底断开连接，不再重连");
                shutdown();
            }
        }, "Fault-Thread").start();
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        new ReconnectClient();
    }

    public void connect() {
        try {
            if (client != null) {
                System.out.println("关闭旧客户端");
                client.shutdownNow();
            }
            client = new AioQuickClient("localhost", 8888, new StringProtocol(), new MessageProcessorImpl());
            session = client.start();
            System.out.println("客户端连接成功");
        } catch (IOException e) {
            System.out.println("启动客户端异常:" + e.getMessage());
            if (client != null) {
                client.shutdownNow();
            }
        }
    }

    public void shutdown() {
        running = false;
        client.shutdownNow();
    }

}
