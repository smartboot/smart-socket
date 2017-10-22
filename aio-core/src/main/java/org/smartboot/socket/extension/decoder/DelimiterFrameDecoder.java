package org.smartboot.socket.extension.decoder;

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
        if (exceptIndex == endFLag.length) {
            buffer.flip();
            finishRead = true;
        }
        return finishRead;
    }

    public ByteBuffer getBuffer() {
        byte[] data = new byte[(bufferList.size() - 1) * bufferList.get(0).capacity() + bufferList.get(bufferList.size() - 1).limit()];
        int index = 0;
        for (ByteBuffer b : bufferList) {
            System.arraycopy(b.array(), b.position(), data, index, b.remaining());
            index += b.remaining();
        }
        return ByteBuffer.wrap(data);
    }

    public void rest(byte[] endFLag) {
        bufferList.clear();
        finishRead = false;
        this.endFLag = endFLag;
    }

}
