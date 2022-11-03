/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DelimiterFrameDecoder.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.decoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 指定结束标识的解码器
 *
 * @author 三刀
 * @version V1.0 , 2017/10/20
 */
public class DelimiterFrameDecoder implements SmartDecoder {
    private final int reposition;
    /**
     * 存储已解析的数据
     */
    private final List<ByteBuffer> bufferList;
    /**
     * 消息结束标志
     */
    private byte[] endFLag;
    /**
     * 期望本次校验的结束标索引位置
     */
    private int exceptIndex;
    /**
     * 是否解析完成
     */
    private boolean finishRead;
    private int position;

    public DelimiterFrameDecoder(byte[] endFLag, int unitBufferSize) {
        if(endFLag == null || endFLag.length == 0){
            throw new IllegalArgumentException("endFLag cannot be empty");
        }
        if(unitBufferSize < 1){
            throw new IllegalArgumentException("unitBufferSize Must be greater than 1");
        }
        this.endFLag = endFLag;
        int p = 0;
        for (int i = 1; i < endFLag.length; i++) {
            if (endFLag[i] != endFLag[0]) {
                p = i - 1;
                break;
            }
        }
        reposition = p;
        bufferList = new ArrayList<>();
        bufferList.add(ByteBuffer.allocate(unitBufferSize));
    }

    public boolean decode(ByteBuffer byteBuffer) {
        if (finishRead) {
            throw new RuntimeException("delimiter has finish read");
        }
        ByteBuffer preBuffer = bufferList.get(position);

        while (byteBuffer.hasRemaining()) {
            if (!preBuffer.hasRemaining()) {
                preBuffer.flip();
                position++;
                if (position < bufferList.size()) {
                    preBuffer = bufferList.get(position);
                    preBuffer.clear();
                } else {
                    preBuffer = ByteBuffer.allocate(preBuffer.capacity());
                    bufferList.add(preBuffer);
                }
            }
            byte data = byteBuffer.get();
            preBuffer.put(data);
            if (data != endFLag[exceptIndex]) {
                //bugfix for https://gitee.com/smartboot/smart-socket/issues/I4H4YB
                if (exceptIndex != reposition + 1 || data != endFLag[reposition]) {
//                    exceptIndex =  0;
                    exceptIndex = endFLag[0] == data ? 1 : 0;
                }
            } else if (++exceptIndex == endFLag.length) {
                preBuffer.flip();
                finishRead = true;
                break;
            }
        }

        return finishRead;
    }

    @Override
    public ByteBuffer getBuffer() {
        if (position == 0) {
            return bufferList.get(position);
        }
        byte[] data = new byte[(position) * bufferList.get(0).capacity() + bufferList.get(position).limit()];
        int index = 0;
        for (int i = 0; i <= position; i++) {
            ByteBuffer b = bufferList.get(i);
            System.arraycopy(b.array(), b.position(), data, index, b.remaining());
            index += b.remaining();
        }
        return ByteBuffer.wrap(data);
    }

    /**
     * 重置解码器
     */
    public void reset() {
        reset(null);
    }

    /**
     * 重置解码器
     *
     * @param endFLag 更新结束标志
     */
    public void reset(byte[] endFLag) {
        if (endFLag != null) {
            this.endFLag = endFLag;
        }
        finishRead = false;
        exceptIndex = 0;
        position = 0;
        bufferList.get(position).clear();
    }
}
