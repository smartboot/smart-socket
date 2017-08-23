package net.vinote.demo;

import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * Created by zhengjunwei on 2017/7/12.
 */
public class IntegerServer {
    public static void main(String[] args) {
        AioQuickServer server = new AioQuickServer()
                .bind(8888)
                .setProtocol(new IntegerProtocol())
                .setProcessor(new IntegerServerProcessor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
