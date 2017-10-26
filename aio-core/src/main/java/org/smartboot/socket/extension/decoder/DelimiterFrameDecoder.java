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

    public boolean decoder(ByteBuffer byteBuffer) {
        if (finishRead) {
            throw new RuntimeException("delimiter has finish read");
        }
        ByteBuffer preBuffer = bufferList.get(bufferList.size() - 1);

        while (byteBuffer.hasRemaining()) {
            if (!preBuffer.hasRemaining()) {
                preBuffer.flip();
                preBuffer = ByteBuffer.allocate(preBuffer.capacity());
                bufferList.add(preBuffer);
            }
            byte data = byteBuffer.get();
            preBuffer.put(data);
            if (data != endFLag[exceptIndex]) {
                exceptIndex = 0;
            } else if (++exceptIndex == endFLag.length) {
                preBuffer.flip();
                finishRead = true;
                break;
            }
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
}
