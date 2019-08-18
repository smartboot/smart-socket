import org.smartboot.socket.udp.MessageProcessor;
import org.smartboot.socket.udp.UdpBootstrap;
import org.smartboot.socket.udp.UdpChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public class UdpServerDemo2 {
    public static void main(String[] args) throws IOException {

        //服务端
        final UdpBootstrap<String> bootstrap = new UdpBootstrap<String>(new StringProtocol(), new MessageProcessor<String>() {
            @Override
            public void process(UdpChannel<String> channel, SocketAddress remote, String msg) {
                InetSocketAddress remoteAddress = (InetSocketAddress) remote;
                if (remoteAddress.getPort() == 9999) {
                    System.out.println(channel + " receive response:" + msg);
                } else {
                    System.out.println("server receive request:" + msg);
                    byte[] b = msg.getBytes();
                    ByteBuffer buffer = ByteBuffer.allocate(4 + b.length);
                    buffer.putInt(b.length);
                    buffer.put(b);
                    buffer.flip();
                    try {
                        channel.write(buffer, remote);
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
                        UdpChannel<String> channel = bootstrap.open();
                        byte[] b = "HelloWorld".getBytes();
                        while (count-- > 0) {
                            ByteBuffer buffer = ByteBuffer.allocate(4 + b.length);
                            buffer.putInt(b.length);
                            buffer.put(b);
                            buffer.flip();
                            channel.write(buffer, remote);
                        }
                        System.out.println("发送完毕");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }
}
