/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MessageProcessor.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.udp;

import java.net.SocketAddress;

/**
 * 消息处理器。
 *
 * @author 三刀
 * @version V1.0.0 2019/8/18
 */
public interface MessageProcessor<Request, Response> {

    /**
     * 处理接收到的消息
     *
     * @param channel 通信会话
     * @param msg     待处理的业务消息
     */
    void process(UdpChannel<Request, Response> channel, SocketAddress remote, Request msg);

}
