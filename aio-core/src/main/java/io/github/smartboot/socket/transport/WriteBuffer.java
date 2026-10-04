/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WriteBuffer.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.transport;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/11/8
 */

public interface WriteBuffer extends Closeable {

    /**
     * 输出一个short类型的数据
     *
     * @param v short数值
     * @throws IOException IO异常
     */
    void writeShort(short v) throws IOException;

    /**
     * @param b 待输出数值
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

    /**
     * 输出指定的ByteBuffer。该buffer从此由框架接管,调用方不得再读写,直至releaseCallback触发。
     * releaseCallback表示框架不再引用该buffer,调用方可安全复用或释放它,
     * 回调保证恰好执行一次(可能是数据已写出,也可能是会话关闭后数据被丢弃)。
     * 回调在IO线程触发。
     * <p>注意:回调触发不代表数据已成功送达对端。</p>
     *
     * @param byteBuffer     待输出的数据
     * @param releaseCallback buffer释放回调,触发后调用方可安全复用该buffer
     */
    void write(ByteBuffer byteBuffer, Runnable releaseCallback);

    void flush();
}