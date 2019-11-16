package org.smartboot.socket.buffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/16
 */
class FastBufferThread extends Thread {
    private final int index;

    public FastBufferThread(Runnable target, String name, int index) {
        super(target, name);
        this.index = index;
        getId();
    }

    public int getIndex() {
        return index;
    }
}
