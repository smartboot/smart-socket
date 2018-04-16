import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Created by 三刀 on 2018/08/23.
 */
public class BytesProtocol implements Protocol<byte[]> {

    // private static final int INT_LENGTH = 4;

    @Override
    public byte[] decode(ByteBuffer data, AioSession<byte[]> session, boolean eof) {
        if (data.remaining() < 1)
            return null;

        // return data.getInt();
        byte[] readBytes = new byte[data.capacity()];
        data.get(readBytes, data.position(), data.limit());
//        System.out.println(data);
        return readBytes;
    }

    @Override
    public ByteBuffer encode(byte[] bytesToWrite, AioSession<byte[]> session) {
        // ByteBuffer b = ByteBuffer.allocate(INT_LENGTH);
        // byte[] bytesPackage = s.getBytes(Charset.forName("UTF-8"));
        // ByteBuffer b = ByteBuffer.wrap(s.getBytes(Charset.forName("UTF-8")));
        // ByteBuffer b = ByteBuffer.allocate(bytesPackage.length);
        // b.putInt(s);
        // b.put(bytesPackage);
        // b.flip();
        ByteBuffer bufferToWrite = ByteBuffer.allocate(1500);
        bufferToWrite.put(bytesToWrite);
        bufferToWrite.flip();
        return bufferToWrite;
    }
}
