/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: PushServer.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.push;

import io.github.smartboot.socket.example.StringProtocol;
import io.github.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/25
 */
public class PushServer {
    public static void main(String[] args) throws IOException {
        AioQuickServer server = new AioQuickServer(8080, new StringProtocol(), new PushServerProcessorMessage());
        server.start();
    }
}
