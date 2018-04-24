package org.smartboot.socket.util;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/23
 */
public class BufferUtils {
    private static final byte SP = 32;
    private static final byte CR = 13;
    private static final byte LF = 10;

    public static void trim(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();

        while (pos < limit) {
            byte b = buffer.get(pos);
            if (b != SP && b != CR && b != LF) {
                break;
            }
            pos++;
        }
        buffer.position(pos);

        while (pos < limit) {
            byte b = buffer.get(limit - 1);
            if (b != SP && b != CR && b != LF) {
                break;
            }
            limit--;
        }
        buffer.limit(limit);
    }

    public static short readUnsignedByte(ByteBuffer buffer) {
        return (short) (buffer.get() & 0xFF);
    }
}
