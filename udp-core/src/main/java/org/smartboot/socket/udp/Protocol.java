/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Protocol.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.udp;

import java.nio.ByteBuffer;

/**
 * <p>
 * 消息传输采用的协议。
 * </p>
 *
 * @author 三刀
 * @version V1.0.0 2018/8/18
 */
public interface Protocol<Request, Response> {
    /**
     * 对于从Socket流中获取到的数据采用当前Protocol的实现类协议进行解析。
     *
     * @param readBuffer 待处理的读buffer
     * @return 本次解码成功后封装的业务消息对象, 返回null则表示解码未完成
     */
    Request decode(final ByteBuffer readBuffer);

    ByteBuffer encode(Response response);
}
