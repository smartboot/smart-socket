package org.smartboot.socket.protocol;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/20
 */
public class FixedLengthFrameDecoder {
    private ByteBuffer buffer;
    private boolean finishRead;

    public FixedLengthFrameDecoder(int frameLength) {
        if (frameLength <= 0) {
            throw new IllegalArgumentException("frameLength must be a positive integer: " + frameLength);
        } else {
            buffer = ByteBuffer.allocate(frameLength);
        }
    }

    public boolean put(byte data) {
        if (finishRead) {
            throw new RuntimeException("delimiter has finish read");
        }
        buffer.put(data);
        if (buffer.hasRemaining()) {
            return false;
        }
        buffer.flip();
        finishRead = true;
        return true;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
