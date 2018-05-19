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
 * 消息过滤器
 * <p>
 *     smart-socket设计的{@code Filter}与状态机{@link StateMachineEnum}看上去比较类似，但两者是以不同的维度对发生的事件进行Filter处理。
 * </p>
 * {@code Filter}以系统服务级别实时过滤如下事件：
 * <ol>
 * <li>connected</li>
 * <li>closed</li>
 * <li>processFail</li>
 * <li>processFilter</li>
 * <li>readFilter</li>
 * <li>writeFilter</li>
 *</ol>
 * @author 三刀
 * @version V1.0.0
 */
public interface Filter<T> {

    /**
     * 建立连接时触发过滤器
     *
     * @param session 新建立的连接session
     */
    void connected(AioSession<T> session);

    /**
     * 网络断链
     *
     * @param session 当前以关闭的session
     */
    void closed(AioSession<T> session);

    /**
     * 数据读取过滤,可用于统计流量
     *
     * @param session 当前执行read的AioSession对象
     * @param readSize 本次解码读取的数据长度
     */
    public void readFilter(AioSession<T> session, int readSize);


    /**
     * 消息处理前置预处理
     *
     * @param session 当前执行消息处理的session对象
     * @param msg     编解码后的消息实体
     */
    public void processFilter(AioSession<T> session, T msg);


    /**
     * 消息接受失败处理
     *
     * @param session   消息处理异常的session对象
     * @param msg     编解码后的消息实体
     * @param e       本次处理异常对象
     */
    public void processFail(AioSession<T> session, T msg, Throwable e);

    /**
     * 数据输出过滤,可用于统计流量
     *
     * @param session
     * @param writeSize 本次输出的数据长度
     */
    public void writeFilter(AioSession<T> session, int writeSize);

}
