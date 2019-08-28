package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/11
 */
public final class ReadEvent {
    /**
     * 当前触发读回调的会话
     */
    private TcpAioSession session;
    /**
     * 本次读取的字节数
     */
    private int readSize;

    public TcpAioSession getSession() {
        return session;
    }

    public void setSession(TcpAioSession session) {
        this.session = session;
    }

    public int getReadSize() {
        return readSize;
    }

    public void setReadSize(int readSize) {
        this.readSize = readSize;
    }
}
