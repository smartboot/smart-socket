/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HeartServer.java
 * Date: 2021-01-21
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.reconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StringProtocol;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class ReconnectServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectServer.class);

    public static void main(String[] args) throws IOException {

        //启动服务
        AioQuickServer<String> server = new AioQuickServer<>(8888, new StringProtocol(), new MessageProcessorImpl());
        server.start();
    }
}
