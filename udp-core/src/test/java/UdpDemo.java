import org.smartboot.socket.udp.MessageProcessor;
import org.smartboot.socket.udp.UdpBootstrap;
import org.smartboot.socket.udp.UdpChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public class UdpDemo {
    public static void main(String[] args) throws IOException, InterruptedException {

        //服务端
        final UdpBootstrap<String, String> bootstrap = new UdpBootstrap<String, String>(new StringProtocol(), new MessageProcessor<String, String>() {
            @Override
            public void process(UdpChannel<String, String> channel, SocketAddress remote, String msg) {
                InetSocketAddress remoteAddress = (InetSocketAddress) remote;
                if (remoteAddress.getPort() == 9999) {
                    System.out.println(channel + " receive response:" + msg);
                } else {
                    System.out.println("server receive request:" + msg);
                    try {
                        channel.write(msg, remote);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        bootstrap.open(9999);
        System.out.println("启动成功");

        //客户端
        int i = 10;
        final SocketAddress remote = new InetSocketAddress("localhost", 9999);
        while (i-- > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int count = 10;
                        UdpChannel<String, String> channel = bootstrap.open();
                        while (count-- > 0) {
                            channel.write("HelloWorld", remote);
                        }
                        System.out.println("发送完毕");
//                        channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
        Thread.sleep(100);
        bootstrap.shutdown();
    }
}
