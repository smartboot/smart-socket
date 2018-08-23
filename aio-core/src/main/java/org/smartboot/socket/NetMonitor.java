/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Filter.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket;

import org.smartboot.socket.transport.AioSession;

/**
 * 网络监控器
 *
 * @author 三刀
 * @version V1.0.0
 */
public interface NetMonitor<T> {


    /**
     * 数据读取过滤,可用于统计流量
     *
     * @param session  当前执行read的AioSession对象
     * @param readSize 本次解码读取的数据长度
     */
    void readMonitor(AioSession<T> session, int readSize);

    /**
     * 数据输出过滤,可用于统计流量
     *
     * @param session   本次执行write回调的AIOSession对象
     * @param writeSize 本次输出的数据长度
     */
    void writeMonitor(AioSession<T> session, int writeSize);

}
