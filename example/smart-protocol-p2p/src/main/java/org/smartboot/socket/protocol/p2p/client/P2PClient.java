package org.smartboot.socket.protocol.p2p.client;

import org.smartboot.ioc.transport.NioQuickClient;
import org.smartboot.socket.protocol.p2p.P2PNioProtocol;
import org.smartboot.socket.protocol.p2p.message.BaseMessage;
import org.smartboot.socket.protocol.p2p.message.DetectMessageReq;
import org.smartboot.socket.protocol.p2p.message.DetectMessageResp;
import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;

import java.util.Properties;

public class P2PClient {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.put(DetectMessageResp.class.getName(), "");
        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
        try {
            messageFactory.loadFromProperties(properties);
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final P2PClientNioMessageProcessor processor = new P2PClientNioMessageProcessor();

        final NioQuickClient<BaseMessage> client = new NioQuickClient<BaseMessage>("118.25.26.239", 8888, new P2PNioProtocol(messageFactory), processor);
        client.setWriteQueueSize(1024);
        client.start();


        long num = 0;
        while (num++ < Integer.MAX_VALUE) {
            DetectMessageReq request = new DetectMessageReq();
            request.setDetect("台州人在杭州:" + num);
            try {
//							DetectMessageResp loginResp = (DetectMessageResp) processor.getSession()
//								.sendWithResponse(request);
                processor.getSession().sendWithoutResponse(request);
//                System.out.println(processor.getSession().sendWithResponse(request));
            } catch (Exception e) {
                System.out.println(num);
                e.printStackTrace();
//                System.exit(0);
            }
        }
        client.shutdown();

    }
}
