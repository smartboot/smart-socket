package org.smartboot.socket.buffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/16
 */
public class FastBufferThread extends Thread {
    private final int threadId;

    public FastBufferThread(Runnable target, String name, int threadId) {
        super(target, name);
        this.threadId = threadId;
        getId();
    }

    public int getThreadId() {
        return threadId;
    }
}
