package org.smartboot.socket.protocol.strategy;

import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.socket.protocol.HttpV2Entity;

/**
 * Post普通表单提交
 *
 * @author Seer
 * @version V1.0 , 2017/9/3
 */
public class ChunkedStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return false;
    }

    @Override
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
        //解析chunkedBlockSize
        if (entity.chunkedBlockSize < 0) {
            entity.dataStream.setEndFLag("\r\n".getBytes());
            if (entity.dataStream.append(b)) {
                int blockSize = NumberUtils.toInt(entity.dataStream.toString(), -1);
                if (blockSize < 0) {
                    throw new RuntimeException("decode block size error:" + entity.dataStream.toString());
                }
                entity.chunkedBlockSize = blockSize;
                entity.dataStream.reset();
                //解码完成
                if (entity.chunkedBlockSize == 0) {
                    try {
                        entity.binaryBuffer.put((byte) -1);//唤醒take
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        } else {
            try {
                entity.binaryBuffer.put(b);
                entity.binReadLength++;
                if (entity.binReadLength == entity.chunkedBlockSize) {
                    entity.binReadLength = 0;
                    entity.chunkedBlockSize = -1;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
