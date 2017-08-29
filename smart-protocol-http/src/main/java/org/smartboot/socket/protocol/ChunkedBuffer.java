package org.smartboot.socket.protocol;

import org.apache.commons.lang.math.NumberUtils;

/**
 * @author Seer
 * @version V1.0 , 2017/8/28
 */
public class ChunkedBuffer {
    DataStream blockSizeStream = new DataStream("\r\n".getBytes());
    int blockSize = -1;
    BinaryBuffer data = new BinaryBuffer(128);

    public void decodeBlockSize() {
        blockSize = NumberUtils.toInt(blockSizeStream.toString(), -1);
        if (blockSize < 0) {
            throw new RuntimeException("decode block size error:" + blockSizeStream.toString());
        }
        blockSizeStream.reset();
    }

}
