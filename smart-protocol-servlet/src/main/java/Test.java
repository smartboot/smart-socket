import org.smartboot.socket.http.HttpBootstrap;
import org.smartboot.socket.http.HttpMessageProcessor;
import org.smartboot.socket.protocol.http.ServletHandler;

import java.util.HashMap;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException {

        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        config.setFilters(new SmartFilter[] { new QuickMonitorTimer<HttpEntity>() });
        HashMap<String, String> arg = new HashMap<String, String>();
        arg.put("warfile", "/Users/zhengjunwei/IdeaProjects/smart-platform/smart-dms/target/smart-dms.war");
        arg.put("useInvoker", "true");
        HttpMessageProcessor processor = new HttpMessageProcessor(System.getProperty("webapps.dir", "./"));
        processor.route("/smartdms", new ServletHandler(arg));
        HttpBootstrap.http(processor);
    }
}
