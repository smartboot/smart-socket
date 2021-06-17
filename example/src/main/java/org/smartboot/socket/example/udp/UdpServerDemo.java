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
import org.smartboot.socket.transport.UdpBootstrap;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/3/1
 */
public class UdpServerDemo {
    public static void main(String[] args) throws IOException {
        UdpBootstrap bootstrap = new UdpBootstrap(new StringProtocol(), (session, msg) -> {
            System.out.println("收到客户端消息: " + msg);
            byte[] bytes = msg.getBytes();
            try {
                session.writeBuffer().writeInt(bytes.length);
                session.writeBuffer().write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        bootstrap.setThreadNum(Runtime.getRuntime().availableProcessors())
                .setReadBufferSize(1024).open(8888);
    }
}
