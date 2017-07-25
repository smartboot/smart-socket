//package net.vinote.demo;
//
//import net.vinote.smart.socket.protocol.Protocol;
//import net.vinote.smart.socket.protocol.ProtocolFactory;
//import net.vinote.smart.socket.service.Session;
//import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;
//import net.vinote.smart.socket.transport.IoSession;
//import net.vinote.smart.socket.transport.nio.NioQuickServer;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
///**
// * Created by zhengjunwei on 2017/7/12.
// */
//public class SimpleServer {
//    public static void main(String[] args) {
//        ProtocolFactory<String> factory = new ProtocolFactory<String>() {
//            @Override
//            public Protocol<String> createProtocol() {
//                return new SimpleProtocol();
//            }
//        };
//        AbstractServerDataGroupProcessor processor = new AbstractServerDataGroupProcessor<String>() {
//
//            @Override
//            public Session<String> initSession(final IoSession<String> session) {
//                return new Session<String>() {
//                    @Override
//                    public void sendWithoutResponse(String requestMsg) throws Exception {
//                        ByteBuffer buffer = ByteBuffer.wrap(requestMsg.getBytes());
//                        buffer.position(buffer.limit());
//                        session.write(buffer);
//                    }
//
//                    @Override
//                    public String sendWithResponse(String requestMsg) throws Exception {
//                        return null;
//                    }
//
//                    @Override
//                    public String sendWithResponse(String requestMsg, long timeout) throws Exception {
//                        return null;
//                    }
//
//                    @Override
//                    public boolean notifySyncMessage(String baseMsg) {
//                        return false;
//                    }
//                };
//            }
//
//            @Override
//            public void process(Session<String> session, String msg) throws Exception {
//                System.out.println("receive:" + msg);
//                session.sendWithoutResponse("Hi,Client\r\n");
//
//            }
//        };
//        NioQuickServer server = new NioQuickServer()
//                .setProtocolFactory(factory)
//                .setProcessor(processor);
//        try {
//            server.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
