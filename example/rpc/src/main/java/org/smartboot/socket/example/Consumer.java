package org.smartboot.socket.example;

import org.smartboot.socket.example.api.DemoApi;
import org.smartboot.socket.example.rpc.RpcConsumerProcessor;
import org.smartboot.socket.example.rpc.RpcProtocol;
import org.smartboot.socket.transport.AioQuickClient;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class Consumer {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        RpcConsumerProcessor rpcConsumerProcessor = new RpcConsumerProcessor();
        AioQuickClient<byte[]> consumer = new AioQuickClient<>("localhost", 8888, new RpcProtocol(), rpcConsumerProcessor);
        consumer.start();

        DemoApi demoApi = rpcConsumerProcessor.getObject(DemoApi.class);
        System.out.println(demoApi.test("smart-socket"));
        System.out.println(demoApi.sum(1, 2));
    }

}
