package net.vinote.demo;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.process.AbstractClientDataProcessor;
import net.vinote.smart.socket.transport.TransportChannel;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

import java.nio.ByteBuffer;

/**
 * Created by zhengjunwei on 2017/7/12.
 */
public class SimpleClient {
    public static void main(String[] args) throws Exception {
        QuicklyConfig<String> config = new QuicklyConfig<String>(false);
        config.setHost("localhost");
        config.setProtocolFactory(new ProtocolFactory<String>() {
            @Override
            public Protocol<String> createProtocol() {
                return new SimpleProtocol();
            }
        });
        AbstractClientDataProcessor processor = new AbstractClientDataProcessor<String>() {


            @Override
            public Session<String> initSession(final TransportChannel<String> transportSession) {
                this.session = new Session<String>() {
                    @Override
                    public void sendWithoutResponse(String requestMsg) throws Exception {
                        ByteBuffer buffer = ByteBuffer.wrap(requestMsg.getBytes());
                        buffer.position(buffer.limit());
                        transportSession.write(buffer);
                    }

                    @Override
                    public String sendWithResponse(String requestMsg) throws Exception {
                        return null;
                    }

                    @Override
                    public String sendWithResponse(String requestMsg, long timeout) throws Exception {
                        return null;
                    }

                    @Override
                    public boolean notifySyncMessage(String baseMsg) {
                        return false;
                    }
                };
                return this.session;
            }

            @Override
            public void process(Session<String> session, String msg) throws Exception {
                System.out.println("Receive:" + msg);
            }
        };
        config.setProcessor(processor);
        NioQuickClient client = new NioQuickClient(config);
        client.start();
        for (int i = 0; i < 10; i++)
            processor.getSession().sendWithoutResponse("Hi,Server\r\n");
        Thread.sleep(1);
    }
}
