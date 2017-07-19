import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.HttpEntity;
import net.vinote.smart.socket.protocol.HttpProtocolFactory;
import net.vinote.smart.socket.protocol.HttpServerMessageProcessor;
import net.vinote.smart.socket.io.nio.NioQuickServer;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {
        QuicklyConfig<HttpEntity> config = new QuicklyConfig<HttpEntity>(true);

        // 定义服务器接受的消息类型以及各类消息对应的处理器
        config.setThreadNum(8);
        config.setProtocolFactory(new HttpProtocolFactory());
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor();
        config.setProcessor(processor);// 定义P2P协议的处理器,可以自定义
        config.setCacheSize(8);
        NioQuickServer<HttpEntity> server = new NioQuickServer<HttpEntity>(config);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
