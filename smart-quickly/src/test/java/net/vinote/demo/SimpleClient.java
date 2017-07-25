package net.vinote.demo;

import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
import net.vinote.smart.socket.transport.IoSession;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

/**
 * Created by zhengjunwei on 2017/7/12.
 */
public class SimpleClient {
    public static void main(String[] args) throws Exception {
        ProtocolFactory<String> factory = new ProtocolFactory<String>() {
            @Override
            public Protocol<String> createProtocol() {
                return new SimpleProtocol();
            }
        };
        final IoSession[] session = new IoSession[1];
        AbstractClientDataProcessor processor = new AbstractClientDataProcessor<String>() {


            @Override
            public void initSession(final IoSession<String> transportSession) {
                session[0] = transportSession;
            }

            @Override
            public void process(IoSession<String> session, String msg) throws Exception {
                System.out.println("Receive:" + msg);
            }

        };
        NioQuickClient client = new NioQuickClient().connect("localhost", 8888)
                .setProcessor(processor)
                .setProtocolFactory(factory);
        client.start();
        Thread.sleep(10);
        for (int i = 0; i < 10; i++) {
            session[0].write("Hi,Server\r\n");
            System.out.println("Write:" + i);
        }
        Thread.sleep(1);
    }
}
