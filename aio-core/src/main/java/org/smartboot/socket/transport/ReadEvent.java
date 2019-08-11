package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/11
 */
public class ReadEvent {
    private AioSession session;
    private int readSize;

    public AioSession getSession() {
        return session;
    }

    public void setSession(AioSession session) {
        this.session = session;
    }

    public int getReadSize() {
        return readSize;
    }

    public void setReadSize(int readSize) {
        this.readSize = readSize;
    }
}
