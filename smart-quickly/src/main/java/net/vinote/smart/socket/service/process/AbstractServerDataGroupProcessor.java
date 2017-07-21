package net.vinote.smart.socket.service.process;

import net.vinote.smart.socket.io.Channel;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.filter.SmartFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public abstract class AbstractServerDataGroupProcessor<T> implements ProtocolDataProcessor<T> {
    private static Logger logger = LogManager.getLogger(AbstractServerDataGroupProcessor.class);
    public static final String SESSION_KEY = "SESSION";
    public static final String SESSION_PROCESS_THREAD = "_PROCESS_THREAD_";
    /**
     * 消息处理线程
     */
    private ServerDataProcessThread[] processThreads;

    private AtomicInteger processThreadIndex = new AtomicInteger(0);
    private QuicklyConfig<T> quickConfig;

    @SuppressWarnings("unchecked")
    @Override
    public void init(QuicklyConfig<T> config) {
        this.quickConfig = config;
        // 启动线程池处理消息
        processThreads = new AbstractServerDataGroupProcessor.ServerDataProcessThread[config.getThreadNum() << 1];
        for (int i = 0; i < processThreads.length; i++) {
            processThreads[i] = new ServerDataProcessThread("ServerProcess-Thread-" + i);
            processThreads[i].setPriority(Thread.MAX_PRIORITY);
            processThreads[i].start();
        }
    }

    @Override
    public boolean receive(Channel<T> session, T entry) {
        ProcessUnit unit = new ProcessUnit(session, entry);
        ServerDataProcessThread processThread = session.getAttribute(SESSION_PROCESS_THREAD);
        //当前Session未绑定处理器,则先进行处理器选举
        if (processThread == null) {
            processThread = processThreads[processThreadIndex.getAndIncrement() % processThreads.length];
            session.setAttribute(SESSION_PROCESS_THREAD, processThread);
        }
        if (processThread.msgQueue.offer(unit)) {
            return true;
        }
        //Session绑定的处理器处理能力不足，由其他处理器辅助
//        for (int i = processThreads.length - 1; i >= 0; i--) {
//            if (processThreads[i].msgQueue.offer(unit)) {
//                return true;
//            }
//        }
        //所有线程都满负荷
        try {
            processThread.msgQueue.put(unit);
        } catch (InterruptedException e) {
            logger.catching(e);
        }

        return true;
    }

    @Override
    public void shutdown() {
        if (processThreads != null && processThreads.length > 0) {
            for (ServerDataProcessThread thread : processThreads) {
                thread.shutdown();
            }
        }
    }

    /**
     * 消息数据元
     *
     * @author zhengjunwei
     */
    final class ProcessUnit {
        Channel<T> session;
        T msg;

        public ProcessUnit(Channel<T> session, T msg) {
            this.session = session;
            this.msg = msg;
        }
    }


    /**
     * 服务端消息处理线程
     *
     * @author zhengjunwei
     */
    final class ServerDataProcessThread extends Thread {
        private volatile boolean running = true;

        public ServerDataProcessThread(String name) {
            super(name);
        }

        /**
         * 消息缓存队列
         */
        private ArrayBlockingQueue<ProcessUnit> msgQueue = new ArrayBlockingQueue<ProcessUnit>(4096);

        @Override
        public void run() {
            while (running) {
                try {
                    ProcessUnit unit = msgQueue.take();
                    SmartFilter<T>[] filters = AbstractServerDataGroupProcessor.this.quickConfig.getFilters();
                    if (filters != null && filters.length > 0) {
                        for (SmartFilter<T> h : filters) {
                            h.processFilter(unit.session, unit.msg);
                        }
                    }
                    Session<T> session = unit.session.getAttribute(SESSION_KEY);
                    if (unit.session.isValid()) {
                        AbstractServerDataGroupProcessor.this.process(session, unit.msg);
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("session invliad,discard message:" + unit.msg);
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * 停止消息处理线程
         */
        public void shutdown() {
            running = false;
            this.interrupt();
        }
    }
}
