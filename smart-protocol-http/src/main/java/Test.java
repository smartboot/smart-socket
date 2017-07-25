import net.vinote.smart.socket.protocol.HttpEntity;
import net.vinote.smart.socket.protocol.HttpProtocol;
import net.vinote.smart.socket.protocol.HttpServerMessageProcessor;
import net.vinote.smart.socket.protocol.Protocol;
import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.transport.nio.NioQuickServer;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {

        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor();
        NioQuickServer<HttpEntity> server = new NioQuickServer<HttpEntity>()
                .setThreadNum(8)
                .setProtocolFactory(new ProtocolFactory<HttpEntity>() {
                    @Override
                    public Protocol<HttpEntity> createProtocol() {
                        return new HttpProtocol();
                    }
                })
                .setProcessor(processor);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
