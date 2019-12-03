package org.smartboot.socket.buffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/16
 */
final class FastBufferThread extends Thread {
    /**
     * 索引标识
     */
    private final int index;

    FastBufferThread(Runnable target, String name, int index) {
        super(target, name);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
