package org.smartboot.socket.example;

import org.smartboot.socket.example.rpc.RpcProtocol;
import org.smartboot.socket.example.rpc.RpcProviderProcessor;
import org.smartboot.socket.example.api.DemoApi;
import org.smartboot.socket.example.api.DemoApiImpl;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class Provider {
    public static void main(String[] args) throws IOException {
        RpcProviderProcessor rpcProviderProcessor = new RpcProviderProcessor();
        AioQuickServer<byte[]> server = new AioQuickServer<>(8888, new RpcProtocol(), rpcProviderProcessor);
        server.start();

        rpcProviderProcessor.publishService(DemoApi.class, new DemoApiImpl());
    }
}
