package org.smartboot.socket.test;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.UdpBootstrap;
import org.smartboot.socket.transport.UdpChannel;
import org.smartboot.socket.transport.WriteBuffer;

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
        final UdpBootstrap<String> bootstrap = new UdpBootstrap<String>(new StringProtocol(), new MessageProcessor<String>() {
            @Override
            public void process(AioSession<String> channel, String msg) {
                try {
                    InetSocketAddress remoteAddress = channel.getRemoteAddress();
                    if (remoteAddress.getPort() == 9999) {
                        System.out.println(channel + " receive response:" + msg);
                    } else {
                        System.out.println("server receive request:" + msg);
                        WriteBuffer buffer = channel.writeBuffer();
                        byte[] bytes = msg.getBytes();
                        buffer.writeInt(bytes.length);
                        buffer.write(bytes);
                        buffer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        });
        int threadNum = Runtime.getRuntime().availableProcessors();
        bootstrap.setThreadNum(threadNum);
        bootstrap.setReadBufferSize(1024);
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
                        AioSession<String> aioSession = channel.connect(remote);
                        WriteBuffer writeBuffer = aioSession.writeBuffer();
                        byte[] msg = "HelloWorld".getBytes();
                        while (count-- > 0) {
                            writeBuffer.writeInt(msg.length);
                            writeBuffer.write(msg);
                            writeBuffer.flush();
                        }
                        System.out.println("发送完毕");
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
