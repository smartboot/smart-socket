package net.vinote.smart.socket.transport.nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.SelectionKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by zhengjunwei on 2017/6/21.
 */
class SessionReadThread extends Thread {
    private static final Logger logger = LogManager.getLogger(SessionReadThread.class);
    private Set<SelectionKey> selectionKeySet = new HashSet<SelectionKey>();
    /**
     * 需要进行数据输出的Session集合
     */
    private ConcurrentLinkedQueue<SelectionKey> newSelectionKeyList1 = new ConcurrentLinkedQueue<SelectionKey>();
    /**
     * 需要进行数据输出的Session集合
     */
    private ConcurrentLinkedQueue<SelectionKey> newSelectionKeyList2 = new ConcurrentLinkedQueue<SelectionKey>();
    /**
     * 需要进行数据输出的Session集合存储控制标，true:newSelectionKeyList1,false:newSelectionKeyList2。由此减少锁竞争
     */
    private boolean switchFlag = false;

    private int waitTime = 1;

    public void notifySession(SelectionKey session) {
        session.interestOps(session.interestOps() & ~SelectionKey.OP_READ);
        if (switchFlag) {
            newSelectionKeyList1.add(session);
        } else {
            newSelectionKeyList2.add(session);
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
            if (selectionKeySet.isEmpty() && newSelectionKeyList1.isEmpty() && newSelectionKeyList2.isEmpty()) {
                synchronized (this) {
                    if (selectionKeySet.isEmpty() && newSelectionKeyList1.isEmpty() && newSelectionKeyList2.isEmpty()) {
                        try {
                            long start = System.currentTimeMillis();
                            this.wait(waitTime);
                            if (waitTime < 2000) {
                                waitTime += 100;
                            } else {
                                waitTime = 0;
                            }
                            if (logger.isTraceEnabled()) {
                                logger.trace("nofity sessionReadThread,waitTime:" + waitTime + " , real waitTime:" + (System.currentTimeMillis() - start));
                            }
                        } catch (InterruptedException e) {
                            logger.catching(e);
                        }
                    }
                }
            }

            if (switchFlag) {
                readSelectionKeyList(newSelectionKeyList2);

            } else {
                readSelectionKeyList(newSelectionKeyList1);
            }
            switchFlag = !switchFlag;

            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                try {
                    NioSession<?> session = (NioSession<?>) key.attachment();
                    //未读到数据则关注读
                    int readSize = session.read(3);
                    switch (readSize) {
                        case -1: {
//                            System.out.println("End Of Stream");
                            session.reachEndOfStream();
//                            session.flushReadBuffer();
                            iterator.remove();
                            if (session.getWriteBuffer() == null) {
                                session.close();
                            }
                            break;
                        }
                        case 0: {
                            if (!session.getReadPause().get()) {
                                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                key.selector().wakeup();//一定要唤醒一次selector
                            }
//                            session.flushReadBuffer();
                            iterator.remove();
                            break;
                        }
                        default: {
//                            session.flushReadBuffer();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    key.cancel();
                    iterator.remove();
                }
                waitTime = 1;
            }

        }
    }

    private void readSelectionKeyList(ConcurrentLinkedQueue<SelectionKey> keyList) {
        while (true) {
            SelectionKey key = keyList.poll();
            if (key == null) {
                break;
            }
            try {
                NioSession<?> session = (NioSession) key.attachment();
                //未读到数据则关注读
                int readSize = session.read(3);
                switch (readSize) {
                    case -1: {
//                        System.out.println("End Of Stream");
                        session.reachEndOfStream();
//                        session.flushReadBuffer();
                        if (session.getWriteBuffer() == null) {
                            session.close();
                        }
                        break;
                    }
                    case 0: {
                        if (!session.getReadPause().get()) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                            key.selector().wakeup();//一定要唤醒一次selector
                        }
//                        session.flushReadBuffer();
                        break;
                    }
                    default: {
                        selectionKeySet.add(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                key.cancel();
            }
        }
    }

}
