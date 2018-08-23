package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.QuickTimerTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> extends QuickTimerTask implements Plugin<T> {
    private static final Logger logger = LoggerFactory.getLogger(MonitorPlugin.class);
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
    public boolean preProcess(AioSession<T> session, T t) {
        processMsgNum.incrementAndGet();
        totleProcessMsgNum.incrementAndGet();
        return true;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, AioSession<T> session, Throwable throwable) {
        switch (stateMachineEnum) {
            case PROCESS_EXCEPTION:
                processFailNum.incrementAndGet();
                break;
            case NEW_SESSION:
                newConnect.incrementAndGet();
                break;
            case SESSION_CLOSED:
                disConnect.incrementAndGet();
                break;
        }
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

    @Override
    public void readMonitor(AioSession<T> session, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        inFlow.addAndGet(readSize);
    }

    @Override
    public void writeMonitor(AioSession<T> session, int writeSize) {
        outFlow.addAndGet(writeSize);
    }
}
