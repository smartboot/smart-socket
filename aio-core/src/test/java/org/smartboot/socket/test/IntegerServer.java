/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerServer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.test;

import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerServer {
    public static void main(String[] args) throws IOException {
        AioQuickServer<Integer> server = new AioQuickServer<Integer>(8888, new IntegerProtocol(), new IntegerServerProcessor());
        server.start();
    }
}
