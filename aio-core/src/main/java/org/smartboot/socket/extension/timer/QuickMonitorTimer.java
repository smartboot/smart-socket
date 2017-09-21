package org.smartboot.socket.extension.timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.service.SmartFilter;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器监测定时器
 * <p>统计一分钟内接收到的数据流量，接受消息数，处理消息数，处理失败消息数</p>
 *
 * @author Seer
 * @version QuickMonitorTimer.java, v 0.1 2015年3月18日 下午11:25:21 Seer Exp.
 */
public class QuickMonitorTimer<T> extends QuickTimerTask implements SmartFilter<T> {
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
    private AtomicInteger processFailNum = new AtomicInteger(0);

    /**
     * 当前周期内处理消息数
     */
    private AtomicInteger processMsgNum = new AtomicInteger(0);


    private volatile long totleProcessMsgNum = 0;

    @Override
    protected long getDelay() {
        return getPeriod();
    }

    @Override
    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    public void processFilter(AioSession<T> session, T d) {
        processMsgNum.incrementAndGet();
        totleProcessMsgNum++;
    }

    public void readFilter(AioSession<T> session, int readSize) {
        inFlow.addAndGet(readSize);
    }

    public void processFailHandler(AioSession<T> session, T d, Exception e) {
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
        int curDiscardNum = processFailNum.getAndSet(0);
        int curProcessMsgNum = processMsgNum.getAndSet(0);
        logger.info("\r\n-----这一分钟发生了什么----\r\n流入流量:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n流出:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n处理失败消息数:\t" + curDiscardNum
                + "\r\n已处理消息量:\t" + curProcessMsgNum
                + "\r\n已处理消息总量:\t" + totleProcessMsgNum);
    }


}
