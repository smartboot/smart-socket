//package net.vinote.demo;
//
//import org.smartboot.socket.protocol.Protocol;
//import org.smartboot.socket.protocol.ProtocolFactory;
//import org.smartboot.socket.service.process.AbstractServerDataGroupProcessor;
//import org.smartboot.socket.transport.IoSession;
//import org.smartboot.socket.transport.aio.AioQuickServer;
//
//import java.io.IOException;
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
//            public void initSession(final IoSession<String> session) {
//
//            }
//
//            @Override
//            public void process(IoSession<String> session, String msg) throws Exception {
//                System.out.println("receive:" + msg);
//                session.write("Hi,Client\r\n");
//
//            }
//        };
//        AioQuickServer server = new AioQuickServer()
//                .setProtocolFactory(factory)
////                .setProcessor(processor)
//                .setFilters(new SimpleMonitorTimer());
//        try {
//            server.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
