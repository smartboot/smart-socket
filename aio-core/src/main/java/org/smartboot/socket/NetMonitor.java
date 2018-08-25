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
 * 网络监控器，提供通讯层面监控功能的接口。
 * <p>
 * smart-socket并未单独提供配置监控服务的接口，用户在使用时仅需在MessageProcessor实现类中同时实现当前NetMonitor接口即可。
 * 在注册消息处理器时，若服务监测到该处理器同时实现了NetMonitor接口，则该监视器便会生效。
 * </p>
 *<h2>示例：</h2>
 * <pre>
 *     public class MessageProcessorImpl implements MessageProcessor,NetMonitor{
 *
 *     }
 * </pre>
 * @author 三刀
 * @version V1.0.0
 */
public interface NetMonitor<T> {


    /**
     * 监控触发本次读回调Session的已读数据字节数
     *
     * @param session  当前执行read的AioSession对象
     * @param readSize 已读数据长度
     */
    void readMonitor(AioSession<T> session, int readSize);

    /**
     * 监控触发本次写回调session的已写数据字节数
     *
     * @param session   本次执行write回调的AIOSession对象
     * @param writeSize 本次输出的数据长度
     */
    void writeMonitor(AioSession<T> session, int writeSize);

}
