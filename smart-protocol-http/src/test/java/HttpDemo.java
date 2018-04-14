import org.smartboot.socket.http.HttpBootstrap;
import org.smartboot.socket.http.HttpMessageProcessor;
import org.smartboot.socket.http.HttpPerformanceProcessor;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/28
 */
public class HttpDemo {
    public static void main(String[] args) {
        HttpPerformanceProcessor processor = new HttpPerformanceProcessor();
        HttpBootstrap.http(processor);
    }
}
