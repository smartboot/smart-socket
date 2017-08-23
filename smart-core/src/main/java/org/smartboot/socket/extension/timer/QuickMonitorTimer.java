package org.smartboot.socket.extension.timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.service.filter.SmartFilter;
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
    private static Logger logger = LogManager.getLogger(QuickMonitorTimer.class);
    /**
     * 当前周期内消息 流量监控
     */
    private AtomicLong flow = new AtomicLong(0);
    /**
     * 当前周期内接受消息数
     */
    private AtomicInteger recMsgnum = new AtomicInteger(0);

    /**
     * 当前周期内处理失败消息数
     */
    private AtomicInteger processFailNum = new AtomicInteger(0);

    /**
     * 当前周期内处理消息数
     */
    private AtomicInteger processMsgNum = new AtomicInteger(0);

    /**
     * 当前积压待处理的消息数
     */
    private AtomicInteger messageStorage = new AtomicInteger(0);

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
        messageStorage.decrementAndGet();
        totleProcessMsgNum++;
    }

    public void readFilter(AioSession<T> session, T d, int readSize) {
        flow.addAndGet(readSize);
        recMsgnum.incrementAndGet();
        messageStorage.incrementAndGet();
    }

    public void processFailHandler(AioSession<T> session, T d) {
        processFailNum.incrementAndGet();
        messageStorage.decrementAndGet();
    }


    @Override
    public void run() {
        long curFlow = flow.getAndSet(0);
        int curRecMsgnum = recMsgnum.getAndSet(0);
        int curDiscardNum = processFailNum.getAndSet(0);
        int curProcessMsgNum = processMsgNum.getAndSet(0);
        logger.info("\r\n-----这一分钟发生了什么----\r\n总流量:\t\t" + curFlow * 1.0 / (1024 * 1024) + "(MB)" + "\r\n请求消息总量:\t"
                + curRecMsgnum + "\r\n平均消息大小:\t" + (curRecMsgnum > 0 ? curFlow * 1.0 / curRecMsgnum : 0)
                + "(B)" + "\r\n处理失败消息数:\t" + curDiscardNum + "\r\n已处理消息量:\t" + curProcessMsgNum
                + "\r\n待处理消息量:\t" + messageStorage.get() + "\r\n已处理消息总量:\t"
                + totleProcessMsgNum);
    }


}
