/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ByteBufferArray.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.nio.ByteBuffer;


/**
 * @author 三刀
 */
final class ByteBufferArray {
    private final ByteBuffer[] buffers;
    private final int offset;
    private final int length;

    public ByteBufferArray(ByteBuffer[] buffers, int offset, int length) {
        this.buffers = buffers;
        this.offset = offset;
        this.length = length;
    }

    public ByteBuffer[] getBuffers() {
        return buffers;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }
}