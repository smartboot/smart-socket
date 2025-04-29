/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteBuffer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/11/8
 */

public interface WriteBuffer extends Closeable {

    default void write(int b) {
        writeByte((byte) b);
    }

    /**
     * 输出一个short类型的数据
     *
     * @param v short数值
     * @throws IOException IO异常
     */
    void writeShort(short v) throws IOException;

    /**
     * @param b 待输出数值
     * @see #write(int)
     */
    void writeByte(byte b);


    /**
     * 输出int数值,占用4个字节
     *
     * @param v int数值
     * @throws IOException IO异常
     */
    void writeInt(int v) throws IOException;

    /**
     * 输出long数值,占用8个字节
     *
     * @param v long数值
     * @throws IOException IO异常
     */
    void writeLong(long v) throws IOException;

    default void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    void write(byte[] b, int off, int len) throws IOException;

    void write(byte[] bytes, int offset, int len, Consumer<WriteBuffer> consumer) throws IOException;

    default void write(byte[] bytes, Consumer<WriteBuffer> consumer) throws IOException {
        write(bytes, 0, bytes.length, consumer);
    }

    void transferFrom(ByteBuffer byteBuffer, Consumer<WriteBuffer> consumer) throws IOException;

    void flush();
}