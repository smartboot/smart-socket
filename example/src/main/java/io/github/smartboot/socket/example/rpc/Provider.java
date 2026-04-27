/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Provider.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.rpc;

import io.github.smartboot.socket.example.rpc.api.DemoApi;
import io.github.smartboot.socket.example.rpc.api.DemoApiImpl;
import io.github.smartboot.socket.example.rpc.rpc.RpcProtocol;
import io.github.smartboot.socket.example.rpc.rpc.RpcProviderProcessor;
import io.github.smartboot.socket.transport.AioQuickServer;

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
