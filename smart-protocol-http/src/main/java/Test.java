import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.protocol.http.HttpEntity;
import org.smartboot.socket.protocol.http.HttpProtocol;
import org.smartboot.socket.protocol.http.process.HttpServerMessageProcessor;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.util.HashMap;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {

        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HashMap<String,String> arg=new HashMap<String, String>();
        arg.put("warfile","/Users/zhengjunwei/IdeaProjects/yt_trade/trade-web/target/dev-trade-web.war");
        HttpServerMessageProcessor processor = new HttpServerMessageProcessor(arg);
        AioQuickServer<HttpEntity> server = new AioQuickServer<HttpEntity>()
                .setThreadNum(8)
                .setProtocol(new HttpProtocol())
                .setFilters(new QuickMonitorTimer<HttpEntity>())
                .setProcessor(processor);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
