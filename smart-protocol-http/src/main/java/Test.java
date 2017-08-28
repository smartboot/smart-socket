import org.smartboot.socket.protocol.HttpEntity;
import org.smartboot.socket.protocol.HttpProtocol;
import org.smartboot.socket.protocol.HttpServerMessageProcessor;
import org.smartboot.socket.protocol.HttpV2Entity;
import org.smartboot.socket.protocol.HttpV2Protocol;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {

        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor();
        AioQuickServer<HttpV2Entity> server = new AioQuickServer<HttpV2Entity>()
                .setThreadNum(8)
                .setProtocol(new HttpV2Protocol())
                .setProcessor(processor);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
