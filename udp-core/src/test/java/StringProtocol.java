import org.smartboot.socket.udp.Protocol;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/16
 */
public class StringProtocol implements Protocol<String, String> {
    @Override
    public String decode(ByteBuffer readBuffer) {
        int remaining = readBuffer.remaining();
        if (remaining < 4) {
            return null;
        }
        readBuffer.mark();
        int length = readBuffer.getInt();
        if (length > readBuffer.remaining()) {
            readBuffer.reset();
            System.out.println("reset");
            return null;
        }
        byte[] b = new byte[length];
        readBuffer.get(b);
        readBuffer.mark();
        return new String(b);
    }

    @Override
    public void encode(ByteBuffer buffer, String response) {
        byte[] b = response.getBytes();
        buffer.putInt(b.length);
        buffer.put(b);
        buffer.flip();
    }
}
