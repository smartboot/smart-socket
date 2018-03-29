/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: QuickMonitorTimer.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.extension.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.Filter;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器监测定时器
 * <p>统计一分钟内接收到的数据流量，接受消息数，处理消息数，处理失败消息数</p>
 *
 * @author 三刀
 * @version QuickMonitorTimer.java, v 0.1 2015年3月18日 下午11:25:21 Seer Exp.
 */
public class QuickMonitorTimer<T> extends QuickTimerTask implements Filter<T> {
    private static final Logger logger = LoggerFactory.getLogger(QuickMonitorTimer.class);
    /**
     * 当前周期内消息 流量监控
     */
    private AtomicLong inFlow = new AtomicLong(0);

    /**
     * 当前周期内消息 流量监控
     */
    private AtomicLong outFlow = new AtomicLong(0);

    /**
     * 当前周期内处理失败消息数
     */
    private AtomicLong processFailNum = new AtomicLong(0);

    /**
     * 当前周期内处理消息数
     */
    private AtomicLong processMsgNum = new AtomicLong(0);


    private AtomicLong totleProcessMsgNum = new AtomicLong(0);

    /**
     * 新建连接数
     */
    private AtomicInteger newConnect = new AtomicInteger(0);

    /**
     * 断链数
     */
    private AtomicInteger disConnect = new AtomicInteger(0);

    /**
     * 在线连接数
     */
    private AtomicInteger onlineCount = new AtomicInteger(0);

    private AtomicInteger totalConnect = new AtomicInteger(0);

    @Override
    protected long getDelay() {
        return getPeriod();
    }

    @Override
    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public void processFilter(AioSession<T> session, T d) {
        processMsgNum.incrementAndGet();
        totleProcessMsgNum.incrementAndGet();
    }

    @Override
    public void connected(AioSession<T> session) {
        newConnect.incrementAndGet();
    }

    @Override
    public void closed(AioSession<T> session) {
        disConnect.incrementAndGet();
    }

    @Override
    public void readFilter(AioSession<T> session, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        inFlow.addAndGet(readSize);
    }

    @Override
    public void processFail(AioSession<T> session, T d, Throwable e) {
        processFailNum.incrementAndGet();
    }

    @Override
    public void writeFilter(AioSession<T> session, int readSize) {
        outFlow.addAndGet(readSize);
    }


    @Override
    public void run() {
        long curInFlow = inFlow.getAndSet(0);
        long curOutFlow = outFlow.getAndSet(0);
        long curDiscardNum = processFailNum.getAndSet(0);
        long curProcessMsgNum = processMsgNum.getAndSet(0);
        int connectCount = newConnect.getAndSet(0);
        int disConnectCount = disConnect.getAndSet(0);
        logger.info("\r\n-----这一分钟发生了什么----\r\n流入流量:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n流出流量:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n处理失败消息数:\t" + curDiscardNum
                + "\r\n已处理消息量:\t" + curProcessMsgNum
                + "\r\n已处理消息总量:\t" + totleProcessMsgNum.get()
                + "\r\n新建连接数:\t" + connectCount
                + "\r\n断开连接数:\t" + disConnectCount
                + "\r\n在线连接数:\t" + onlineCount.addAndGet(connectCount - disConnectCount)
                + "\r\n总连接次数:\t" + totalConnect.addAndGet(connectCount));
    }

}
