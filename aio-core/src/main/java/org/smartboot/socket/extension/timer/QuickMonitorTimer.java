/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: QuickMonitorTimer.java
 * Date: 2017-11-25 10:29:55
 * Author: sandao
 */

package org.smartboot.socket.extension.timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Filter;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器监测定时器
 * <p>统计一分钟内接收到的数据流量，接受消息数，处理消息数，处理失败消息数</p>
 *
 * @author 三刀
 * @version QuickMonitorTimer.java, v 0.1 2015年3月18日 下午11:25:21 Seer Exp.
 */
public class QuickMonitorTimer<T> extends QuickTimerTask implements Filter<T> {
    private static final Logger logger = LogManager.getLogger(QuickMonitorTimer.class);
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
    public void readFilter(AioSession<T> session, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        inFlow.addAndGet(readSize);
    }

    @Override
    public void processFailHandler(AioSession<T> session, T d, Throwable e) {
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
        logger.info("\r\n-----这一分钟发生了什么----\r\n流入流量:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n流出流量:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n处理失败消息数:\t" + curDiscardNum
                + "\r\n已处理消息量:\t" + curProcessMsgNum
                + "\r\n已处理消息总量:\t" + totleProcessMsgNum.get());
    }


}
