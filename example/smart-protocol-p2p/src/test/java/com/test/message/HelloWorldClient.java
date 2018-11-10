//package com.test.message;
//
//import org.smartboot.socket.protocol.P2PProtocolFactory;
//import org.smartboot.socket.protocol.p2p.client.P2PClientMessageProcessor;
//import org.smartboot.socket.protocol.p2p.message.BaseMessage;
//import org.smartboot.socket.protocol.p2p.message.P2pServiceMessageFactory;
//import org.smartboot.socket.transport.nio.NioQuickClient;
//
//import java.util.Properties;
//
//public class HelloWorldClient {
//    public static void main(String[] args) throws Exception {
//        Properties properties = new Properties();
//        properties.put(HelloWorldResp.class.getName(), "");
//        P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
//        messageFactory.loadFromProperties(properties);
//        P2PClientMessageProcessor processor = new P2PClientMessageProcessor(messageFactory);
//        NioQuickClient<BaseMessage> client = new NioQuickClient<BaseMessage>().connect("localhost", 8888)
//                .setTimeout(1000)
////                .setProcessor(processor)
//                .setProtocolFactory(new P2PProtocolFactory(messageFactory));
//        client.start();
//        int num = 10;
//        while (num-- > 0) {
//            HelloWorldReq req = new HelloWorldReq();
//            req.setName("seer" + num);
//            req.setAge(num);
//            req.setMale(num % 2 == 0);
//            BaseMessage msg = processor.getSession().sendWithResponse(req);
//            System.out.println(msg);
//        }
//        client.shutdown();
//    }
//}
