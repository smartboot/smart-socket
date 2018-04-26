package org.smartboot.socket.mqtt;

import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/24
 */
public class MqttServerBootstrap {
    public static void main(String[] args) {
        AioQuickServer server=new AioQuickServer(1883,new MqttProtocol(),new MqttServerProcessor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
