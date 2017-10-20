package org.smartboot.socket.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/20
 */
public class DelimiterFrameDecoder {
    private byte[] endFLag;
    private int exceptIndex;
    private List<ByteBuffer> bufferList;
    private boolean finishRead;

    public DelimiterFrameDecoder(byte[] endFLag, int unitBufferSize) {
        this.endFLag = endFLag;
        bufferList = new ArrayList<>();
        bufferList.add(ByteBuffer.allocate(unitBufferSize));
    }

    public boolean put(byte data) {
        if (finishRead) {
            throw new RuntimeException("delimiter has finish read");
        }
        ByteBuffer buffer = bufferList.get(bufferList.size() - 1);
        if (!buffer.hasRemaining()) {
            buffer.flip();
            buffer = ByteBuffer.allocate(buffer.capacity());
            bufferList.add(buffer);
        }
        buffer.put(data);
        if (data != endFLag[exceptIndex]) {
            exceptIndex = 0;
            return false;
        } else {
            exceptIndex++;
        }
        boolean result = exceptIndex == endFLag.length;
        if (result) {
            buffer.flip();
            finishRead = true;
        }
        return result;
    }

    public ByteBuffer getBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(bufferList.size() * bufferList.get(0).capacity());
        for (ByteBuffer b : bufferList) {
            buffer.put(b);
        }
        buffer.flip();
        return buffer;
    }

    public void rest(byte[] endFLag) {
        bufferList.clear();
        finishRead = false;
        this.endFLag = endFLag;
    }

}
