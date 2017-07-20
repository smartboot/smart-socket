package net.vinote.smart.socket.io.nio;

import net.vinote.smart.socket.enums.ChannelStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Socket写操作的处理线程。不通过NIO的写关注触发，因为发现效率并不高。而是由该线程进行监控，增强数据输出能力
 * Created by zhengjunwei on 2017/6/14.
 */
public class SessionWriteThread2 extends Thread {
    private static final Logger logger = LogManager.getLogger(SessionWriteThread2.class);
    private Map<NioChannel, AtomicInteger> sessionMap = new ConcurrentHashMap<NioChannel, AtomicInteger>();

    private int waitTime = 1;

    public void notifySession(NioChannel session) {
        AtomicInteger notifyTimes = sessionMap.get(session);
        if (notifyTimes == null) {
            synchronized (session) {
                notifyTimes = sessionMap.get(session);
                if (notifyTimes == null) {
                    notifyTimes = new AtomicInteger(1);
                    sessionMap.put(session, notifyTimes);
                } else {
                    notifyTimes.incrementAndGet();
                }
            }
        } else {
            notifyTimes.incrementAndGet();
        }

        if (sessionMap.get(session) == null) {
            synchronized (session) {
                if (sessionMap.get(session) == null) {
                    sessionMap.put(session, notifyTimes);
                }
            }
        }
        if (waitTime == 0 || waitTime > 10) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (sessionMap.isEmpty()) {
                synchronized (this) {
                    if (sessionMap.isEmpty()) {
                        try {
                            this.wait(waitTime);
                            if (waitTime < 2000) {
                                waitTime++;
                            } else {
                                waitTime = 0;
                            }
                        } catch (InterruptedException e) {
                            logger.catching(e);
                        }
                    }
                }
            }

            for (Map.Entry<NioChannel, AtomicInteger> entry : sessionMap.entrySet()) {
                NioChannel session = entry.getKey();
                AtomicInteger notifyTimes = entry.getValue();
                if (notifyTimes.get() > 1) {
                    notifyTimes.set(1);
                }
                try {
                    session.flushWriteBuffer(3);
                    if (session.getWriteBuffer() == null) {
                        if (session.getStatus() == ChannelStatusEnum.CLOSING) {
                            session.close();
                        }
                        if (notifyTimes.decrementAndGet() <= 0) {//理论上不会小于0
                            sessionMap.remove(session);
                            if (notifyTimes.get() > 0) {
                                sessionMap.put(session, notifyTimes);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.fillInStackTrace();
                    session.close();
                    sessionMap.remove(session);
                }
                waitTime = 1;
            }
        }
    }
}
