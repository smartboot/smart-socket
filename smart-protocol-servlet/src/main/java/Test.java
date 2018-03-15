import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.protocol.http.HttpProtocol;
import org.smartboot.socket.protocol.http.process.HttpServerMessageProcessor;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.util.HashMap;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {

        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HashMap<String, String> arg = new HashMap<String, String>();
        arg.put("warfile", "/Users/zhengjunwei/IdeaProjects/smart-platform/smart-dms/target/smart-dms.war");
        arg.put("useInvoker", "true");
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor(arg);
        AioQuickServer<WinstoneRequest> server = new AioQuickServer<WinstoneRequest>(8888, new HttpProtocol(), processor)
                .setThreadNum(8)
                .setFilters(new QuickMonitorTimer<WinstoneRequest>());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
