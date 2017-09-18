package org.smartboot.socket.transport.disruptor;

import org.smartboot.socket.transport.AioSession;

/**
 * @author Seer
 * @version V1.0 , 2017/9/18
 */
public class ReadEvent {
    private AioSession aioSession;

    public AioSession getAioSession() {
        return aioSession;
    }

    public void setAioSession(AioSession aioSession) {
        this.aioSession = aioSession;
    }
}
