/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpDemo.java
 * Date: 2021-03-01
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.udp;

import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/1
 */
public class UdpClientDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        UdpBootstrap bootstrap = new UdpBootstrap(new StringProtocol(), (session, msg) -> {
            System.out.println("收到服务端消息: " + msg);
        });
        bootstrap.setBannerEnabled(false)
                .setThreadNum(Runtime.getRuntime().availableProcessors())
                .setReadBufferSize(1024);
        AioSession session = bootstrap.open().connect("localhost", 8888);
        byte[] bytes = "hello smart-socket".getBytes();
        for (int i = 0; i < 100; i++) {
            session.writeBuffer().writeInt(bytes.length);
            session.writeBuffer().write(bytes);
            session.writeBuffer().flush();
        }
        Thread.sleep(1000);
        bootstrap.shutdown();
    }
}
