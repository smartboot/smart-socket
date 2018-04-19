import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;
import java.net.StandardSocketOptions;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class BytesServer {
    public static void main(String[] args) {
        AioQuickServer<byte[]> server = new AioQuickServer<byte[]>(8888, new BytesProtocol(),
                new BytesServerProcessor());

        server.setReadBufferSize(1500);
        server.setHost("localhost");
        server.setOption(StandardSocketOptions.SO_REUSEADDR,true);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
