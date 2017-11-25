/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Protocol.java
 * Date: 2017-11-25 10:29:55
 * Author: sandao
 */

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * 消息传输采用的协议
 * 框架本身的所有Socket链路复用同一个Protocol，请勿在其实现类的成员变量中存储特定链路的数据
 *
 * @author 三刀
 * @version Protocol.java, v 0.1 2015年3月13日 下午3:30:57 Seer Exp.
 */
public interface Protocol<T> {
    /**
     * 对于从Socket流中获取到的数据采用当前Protocol的实现类协议进行解析
     *
     * @param data
     * @param session
     * @param eof     是否EOF
     * @return 本次解码所成功解析的消息实例集合, 返回null则表示解码未完成
     */
    public T decode(ByteBuffer data, AioSession<T> session, boolean eof);

    /**
     * 将业务消息实体编码成ByteBuffer用于输出至对端。
     * <b>切勿在encode中直接调用session.write,编码后的byteuffer需交由框架本身来输出</b>
     *
     * @param msg
     * @param session
     * @return
     */
    public ByteBuffer encode(T msg, AioSession<T> session);
}
