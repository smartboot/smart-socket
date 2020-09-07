/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MonitorPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> extends AbstractPlugin<T> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MonitorPlugin.class);
    /**
     * 当前周期内消息 流量监控
     */
    private final LongAdder inFlow = new LongAdder();
    /**
     * 当前周期内消息 流量监控
     */
    private final LongAdder outFlow = new LongAdder();
    /**
     * 当前周期内处理失败消息数
     */
    private final LongAdder processFailNum = new LongAdder();
    /**
     * 当前周期内处理消息数
     */
    private final LongAdder processMsgNum = new LongAdder();
    private final LongAdder totleProcessMsgNum = new LongAdder();
    /**
     * 新建连接数
     */
    private final LongAdder newConnect = new LongAdder();
    /**
     * 断链数
     */
    private final LongAdder disConnect = new LongAdder();
    private final LongAdder totalConnect = new LongAdder();
    private final LongAdder readCount = new LongAdder();
    private final LongAdder writeCount = new LongAdder();
    /**
     * 任务执行频率
     */
    private int seconds;
    /**
     * 在线连接数
     */
    private long onlineCount;

    public MonitorPlugin() {
        this(60);
    }

    public MonitorPlugin(int seconds) {
        this.seconds = seconds;
        long mills = TimeUnit.SECONDS.toMillis(seconds);
        QuickTimerTask.scheduleAtFixedRate(this, mills, mills);
    }


    @Override
    public boolean preProcess(AioSession session, T t) {
        processMsgNum.increment();
        totleProcessMsgNum.increment();
        return true;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession session, Throwable throwable) {
        switch (stateMachineEnum) {
            case PROCESS_EXCEPTION:
                processFailNum.increment();
                break;
            case NEW_SESSION:
                newConnect.increment();
                break;
            case SESSION_CLOSED:
                disConnect.increment();
                break;
            default:
                //ignore other state
                break;
        }
    }

    @Override
    public void run() {
        long curInFlow = getAndReset(inFlow);
        long curOutFlow = getAndReset(outFlow);
        long curDiscardNum = getAndReset(processFailNum);
        long curProcessMsgNum = getAndReset(processMsgNum);
        long connectCount = getAndReset(newConnect);
        long disConnectCount = getAndReset(disConnect);
        onlineCount += connectCount - disConnectCount;
        logger.info("\r\n-----" + seconds + "seconds ----\r\ninflow:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\noutflow:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\nprocess fail:\t" + curDiscardNum
                + "\r\nprocess success:\t" + curProcessMsgNum
                + "\r\nprocess total:\t" + totleProcessMsgNum.longValue()
                + "\r\nread count:\t" + getAndReset(readCount) + "\twrite count:\t" + getAndReset(writeCount)
                + "\r\nconnect count:\t" + connectCount
                + "\r\ndisconnect count:\t" + disConnectCount
                + "\r\nonline count:\t" + onlineCount
                + "\r\nconnected total:\t" + getAndReset(totalConnect)
                + "\r\nRequests/sec:\t" + curProcessMsgNum * 1.0 / seconds
                + "\r\nTransfer/sec:\t" + (curInFlow * 1.0 / (1024 * 1024) / seconds) + "(MB)");
    }

    private long getAndReset(LongAdder longAdder) {
        long result = longAdder.longValue();
        longAdder.add(-result);
        return result;
    }

    @Override
    public void afterRead(AioSession session, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        inFlow.add(readSize);
    }

    @Override
    public void beforeRead(AioSession session) {
        readCount.increment();
    }

    @Override
    public void afterWrite(AioSession session, int writeSize) {
        outFlow.add(writeSize);
    }

    @Override
    public void beforeWrite(AioSession session) {
        writeCount.increment();
    }
}
