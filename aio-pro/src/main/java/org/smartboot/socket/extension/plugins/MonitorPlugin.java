/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MonitorPlugin.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> extends AbstractPlugin<T> implements Runnable {
    /**
     * 当前周期内流入字节数
     */
    private final LongAdder inFlow = new LongAdder();
    /**
     * 当前周期内流出字节数
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
    /**
     * 当前周期内新建连接数
     */
    private final LongAdder newConnect = new LongAdder();
    /**
     * 当前周期内断开连接数
     */
    private final LongAdder disConnect = new LongAdder();
    /**
     * 当前周期内执行 read 操作次数
     */
    private final LongAdder readCount = new LongAdder();
    /**
     * 当前周期内执行 write 操作次数
     */
    private final LongAdder writeCount = new LongAdder();
    /**
     * 任务执行频率
     */
    private final int seconds;
    /**
     * 自插件启用起的累计连接总数
     */
    private long totalConnect;
    /**
     * 自插件启用起的累计处理消息总数
     */
    private long totalProcessMsgNum = 0;
    /**
     * 当前在线状态连接数
     */
    private long onlineCount;

    private final boolean udp;

    public MonitorPlugin() {
        this(60);
    }

    public MonitorPlugin(int seconds) {
        this(seconds, false);
    }

    public MonitorPlugin(int seconds, boolean udp) {
        this.seconds = seconds;
        this.udp = udp;
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(this, seconds, TimeUnit.SECONDS);
    }


    @Override
    public boolean preProcess(AioSession session, T t) {
        processMsgNum.increment();
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
        long curReadCount = getAndReset(readCount);
        long curWriteCount = getAndReset(writeCount);
        onlineCount += connectCount - disConnectCount;
        totalProcessMsgNum += curProcessMsgNum;
        totalConnect += connectCount;
        System.out.println("\r\n-----" + seconds + "seconds ----\r\ninflow:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\noutflow:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\nprocess fail:\t" + curDiscardNum
                + "\r\nprocess count:\t" + curProcessMsgNum
                + "\r\nprocess total:\t" + totalProcessMsgNum
                + "\r\nread count:\t" + curReadCount + "\twrite count:\t" + curWriteCount

                + (udp ? "" : "\r\nconnect count:\t" + connectCount
                + "\r\ndisconnect count:\t" + disConnectCount
                + "\r\nonline count:\t" + onlineCount
                + "\r\nconnected total:\t" + totalConnect)
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
            System.err.println("readSize is 0");
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
