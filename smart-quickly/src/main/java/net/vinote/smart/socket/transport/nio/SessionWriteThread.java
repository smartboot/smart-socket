package net.vinote.smart.socket.transport.nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zhengjunwei on 2017/6/14.
 */
public class SessionWriteThread extends Thread {
    private static final Logger logger = LogManager.getLogger(SessionWriteThread.class);
    private Set<NioSession> sessionSet = new HashSet<NioSession>();
    private Set<NioSession> newSessionSet1 = new HashSet<NioSession>();
    private Set<NioSession> newSessionSet2 = new HashSet<NioSession>();
    private volatile boolean switchFlag = false;

    private volatile int waitTime = 100;

    public void notifySession(NioSession session) {
        if (switchFlag) {
            newSessionSet1.add(session);
        } else {
            newSessionSet2.add(session);
        }
        if (waitTime > 100) {
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
                            long start=System.currentTimeMillis();
                            this.wait(waitTime);
                            if (logger.isTraceEnabled()) {
                                logger.trace("nofity sessionWriteThread,waitTime:" + waitTime+" , real waitTime:"+(System.currentTimeMillis()-start));
                            }
                        } catch (InterruptedException e) {
                           logger.catching(e);
                        }
                    }
                }
            }
            if (switchFlag) {
                sessionSet.addAll(newSessionSet2);
                newSessionSet2.clear();
            } else {
                sessionSet.addAll(newSessionSet1);
                newSessionSet1.clear();
            }
            switchFlag = !switchFlag;

            Iterator<NioSession> iterator = sessionSet.iterator();
            while (iterator.hasNext()) {
                NioSession session = iterator.next();
                try {
                    session.flushWriteBuffer(3);
                    if (session.getWriteBuffer() == null) {
                        iterator.remove();
                    }
                } catch (Exception e) {
                    session.close();
                    iterator.remove();
                }
                waitTime = 0;
            }

            if (waitTime < 2000) {
                waitTime += 100;
            }
        }
    }
}
