import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ChatterClient extends Thread {

    private DatagramSocket s;
    private InetAddress hostAddress;
    private byte[] buf = new byte[1000];
    private DatagramPacket dp = new DatagramPacket(buf, buf.length);
    private int id;

    public ChatterClient(int identifier) {
        id = identifier;
        try {
            s = new DatagramSocket();
            hostAddress = InetAddress.getByName("localhost");

        } catch (UnknownHostException e) {
            System.err.println("Cannot find host");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("Can't open socket");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("ChatterClient starting");
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new ChatterClient(i).start();
        }
    }

    public void run() {
        try {
            for (int i = 0; i < 25; i++) {
                String outMessage = "Client #" + id + ",message #" + i;
                s.send(Dgram.toDatagram(outMessage, hostAddress,
                        9999));
                s.receive(dp);
                String rcvd = "Client #" + id + ",rcvd from " + dp.getAddress()
                        + ", " + dp.getPort() + ":" + Dgram.toString(dp);
                System.out.println(rcvd);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static class Dgram {

        public static DatagramPacket toDatagram(String s, InetAddress destIA,
                                                int destPort) {
            byte[] buf = s.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(4 + buf.length);
            buffer.putInt(buf.length);
            buffer.put(buf);
            return new DatagramPacket(buffer.array(), buffer.capacity(), destIA, destPort);
        }

        public static String toString(DatagramPacket p) {
            return new String(p.getData(), 0, p.getLength());
        }
    }
}