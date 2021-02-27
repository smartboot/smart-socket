/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Provider.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.rpc;

import org.smartboot.socket.example.rpc.api.DemoApi;
import org.smartboot.socket.example.rpc.api.DemoApiImpl;
import org.smartboot.socket.example.rpc.rpc.RpcProtocol;
import org.smartboot.socket.example.rpc.rpc.RpcProviderProcessor;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class Provider {
    public static void main(String[] args) throws IOException {
        RpcProviderProcessor rpcProviderProcessor = new RpcProviderProcessor();
        AioQuickServer server = new AioQuickServer(8888, new RpcProtocol(), rpcProviderProcessor);
        server.start();

        rpcProviderProcessor.publishService(DemoApi.class, new DemoApiImpl());
    }
}
