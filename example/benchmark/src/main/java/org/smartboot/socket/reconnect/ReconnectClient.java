/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HeartClient.java
 * Date: 2021-01-21
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.reconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class ReconnectClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectClient.class);
    private AioQuickClient<String> client;
    private AioSession session;
    private boolean running = true;

    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("启动连接监测");
                while (running) {
                    if (session == null || session.isInvalid()) {
                        LOGGER.info("连接异常，准备重连...");
                        connect();
                    } else {
                        LOGGER.info("连接正常...");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                LOGGER.info("终止连接监测");
            }
        },"Reconnect-Thread").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("模拟连接异常");
                int i = 0;
                while (i++ < 3) {
                    LOGGER.info("第 {} 次断开连接", i);
                    if (session != null) {
                        session.close();
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                LOGGER.info("彻底断开连接，不再重连");
                shutdown();
            }
        },"Fault-Thread").start();
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        new ReconnectClient();
    }

    public void connect() {
        try {
            if (client != null) {
                LOGGER.info("关闭旧客户端");
                client.shutdownNow();
            }
            client = new AioQuickClient<>("localhost", 8888, new StringProtocol(), new MessageProcessorImpl());
            session = client.start();
            LOGGER.info("客户端连接成功");
        } catch (IOException e) {
            LOGGER.error("启动客户端异常:{}", e.getMessage());
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
