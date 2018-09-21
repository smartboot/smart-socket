/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Protocol.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * <p>
 * 消息传输采用的协议。
 * </p>
 * <p>
 * 根据通信双方约定的协议规范实现{@code Protocol}接口，使用时将该实现类注册至服务启动类{@link org.smartboot.socket.transport.AioQuickClient}、{@link org.smartboot.socket.transport.AioQuickServer}。
 * </p>
 * <b>
 * 注意：框架本身的所有Socket链路复用同一个Protocol，请勿在其实现类的成员变量中存储特定链路的数据。
 * </b>
 * <p>
 * <h2>示例：</h2>
 * <pre>
 * public class IntegerProtocol implements Protocol<Integer> {
 *
 *     private static final int INT_LENGTH = 4;
 *
 *     public Integer decode(ByteBuffer data, AioSession<Integer> session, boolean eof) {
 *         if (data.remaining() < INT_LENGTH)
 *             return null;
 *         return data.getInt();
 *     }
 *
 *     public ByteBuffer encode(Integer s, AioSession<Integer> session) {
 *         ByteBuffer b = ByteBuffer.allocate(INT_LENGTH);
 *         b.putInt(s);
 *         b.flip();
 *         return b;
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author 三刀
 * @version V1.0.0 2018/5/19
 */
public interface Protocol<T> {
    /**
     * 对于从Socket流中获取到的数据采用当前Protocol的实现类协议进行解析。
     *
     * <p>
     * 实现的解码方法要尽可能读取readBuffer已有的数据。若不及时解析并最终导致readBuffer无可用空间（即readBuffer.remaining==0），smart-socket会反复触发decode方法形成类似死循环的效果。
     * </p>
     *
     * @param readBuffer 待处理的读buffer
     * @param session    本次需要解码的session
     * @return 本次解码成功后封装的业务消息对象, 返回null则表示解码未完成
     */
    T decode(final ByteBuffer readBuffer, AioSession<T> session);

    /**
     * 将业务消息实体编码成ByteBuffer用于输出至对端。
     * <b>且勿在encode中直接调用session.write,编码后的byteBuffer需交由smart-socket框架本身来输出</b>
     *
     * @param msg     待编码的业务消息
     * @param session 当前执行encode的AIOSession对象
     * @return 将业务消息编码成ByteBuffer
     */
    ByteBuffer encode(T msg, AioSession<T> session);
}
