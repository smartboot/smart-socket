package org.smartboot.socket.extension.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.QuickTimerTask;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> implements Runnable, Plugin<T> {
    private static final Logger logger = LoggerFactory.getLogger(MonitorPlugin.class);
    /**
     * 任务执行频率
     */
    private int seconds = 0;
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

    private AtomicInteger readCount = new AtomicInteger(0);

    private AtomicInteger writeCount = new AtomicInteger(0);

    public MonitorPlugin() {
        this(60);
    }

    public MonitorPlugin(int seconds) {
        this.seconds = seconds;
        long mills = TimeUnit.SECONDS.toMillis(seconds);
        QuickTimerTask.scheduleAtFixedRate(this, mills, mills);
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
            default:
                //ignore other state
                break;
        }
    }

    @Override
    public void run() {
        long curInFlow = inFlow.getAndSet(0);
        long curOutFlow = outFlow.getAndSet(0);
        long curDiscardNum = processFailNum.getAndSet(0);
        long curProcessMsgNum = processMsgNum.getAndAdd(-processMsgNum.get());
        int connectCount = newConnect.getAndAdd(-newConnect.get());
        int disConnectCount = disConnect.getAndAdd(-disConnect.get());
        logger.info("\r\n-----这" + seconds + "秒发生了什么----\r\ninflow:\t\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\noutflow:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\nprocess fail:\t" + curDiscardNum
                + "\r\nprocess success:\t" + curProcessMsgNum
                + "\r\nprocess total:\t" + totleProcessMsgNum.get()
                + "\r\nread count:\t" + readCount.getAndSet(0) + "\twrite count:\t" + writeCount.getAndSet(0)
                + "\r\nconnect count:\t" + connectCount
                + "\r\ndisconnect count:\t" + disConnectCount
                + "\r\nonline count:\t" + onlineCount.addAndGet(connectCount - disConnectCount)
                + "\r\nconnected total:\t" + totalConnect.addAndGet(connectCount)
                + "\r\nRequests/sec:\t" + curProcessMsgNum * 1.0 / seconds
                + "\r\nTransfer/sec:\t" + (curInFlow * 1.0 / (1024 * 1024) / seconds) + "(MB)");
    }

    @Override
    public boolean shouldAccept(AsynchronousSocketChannel channel) {
        return true;
    }

    @Override
    public void afterRead(AioSession<T> session, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        inFlow.addAndGet(readSize);
    }

    @Override
    public void beforeRead(AioSession<T> session) {
        readCount.incrementAndGet();
    }

    @Override
    public void afterWrite(AioSession<T> session, int writeSize) {
        outFlow.addAndGet(writeSize);
    }

    @Override
    public void beforeWrite(AioSession<T> session) {
        writeCount.incrementAndGet();
    }
}
