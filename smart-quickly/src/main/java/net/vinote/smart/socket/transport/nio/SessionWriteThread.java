package net.vinote.smart.socket.transport.nio;

import net.vinote.smart.socket.transport.enums.SessionStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Socket写操作的处理线程。不通过NIO的写关注触发，因为发现效率并不高。而是由该线程进行监控，增强数据输出能力
 * Created by zhengjunwei on 2017/6/14.
 */
public class SessionWriteThread extends Thread {
    private static final Logger logger = LogManager.getLogger(SessionWriteThread.class);
    private List<NioSession> sessionSet = new ArrayList<NioSession>();
    /**
     * 需要进行数据输出的Session集合
     */
    private Set<NioSession> newSessionSet1 = new HashSet<NioSession>();
    /**
     * 需要进行数据输出的Session集合
     */
    private Set<NioSession> newSessionSet2 = new HashSet<NioSession>();
    /**
     * 需要进行数据输出的Session集合存储控制标，true:newSessionSet1,false:newSessionSet2。由此减少锁竞争
     */
    private boolean switchFlag = false;

    private int waitTime = 1;

    public void notifySession(NioSession session) {
        if (switchFlag) {
            synchronized (newSessionSet1) {
                newSessionSet1.add(session);
            }
        } else {
            synchronized (newSessionSet2) {
                newSessionSet2.add(session);
            }
        }
        if (waitTime != 1) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (sessionSet.isEmpty() && newSessionSet1.isEmpty() && newSessionSet2.isEmpty()) {
                synchronized (this) {
                    if (sessionSet.isEmpty() && newSessionSet1.isEmpty() && newSessionSet2.isEmpty()) {
                        try {
                            long start = System.currentTimeMillis();
                            this.wait(waitTime);
                            if (waitTime < 2000) {
                                waitTime++;
                            } else {
                                waitTime = 0;
                            }
                            if (logger.isTraceEnabled()) {
                                logger.trace("nofity sessionWriteThread,waitTime:" + waitTime + " , real waitTime:" + (System.currentTimeMillis() - start));
                            }
                        } catch (InterruptedException e) {
                            logger.catching(e);
                        }
                    }
                }
            }
            if (switchFlag) {
                synchronized (newSessionSet2) {
                    sessionSet.addAll(newSessionSet2);
                    newSessionSet2.clear();
                }
            } else {
                synchronized (newSessionSet1) {
                    sessionSet.addAll(newSessionSet1);
                    newSessionSet1.clear();
                }
            }
            switchFlag = !switchFlag;

            Iterator<NioSession> iterator = sessionSet.iterator();
            Set<NioSession> removeSession = new HashSet<NioSession>();
            while (iterator.hasNext()) {
                NioSession session = iterator.next();
                try {
                    session.flushWriteBuffer(3);
                    if (session.getWriteBuffer() == null) {
                        removeSession.add(session);
                        if(session.getStatus()== SessionStatusEnum.CLOSING){
                            session.close();
                        }
                    }
                } catch (Exception e) {
                    e.fillInStackTrace();
                    session.close();
                    removeSession.add(session);
                }
                waitTime = 1;
            }
            sessionSet.removeAll(removeSession);
        }
    }
}
