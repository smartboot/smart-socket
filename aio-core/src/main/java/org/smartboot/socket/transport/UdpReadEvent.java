package org.smartboot.socket.transport;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/18
 */
final class UdpReadEvent<Request> {

    /**
     * UDP会话
     */
    private UdpAioSession<Request> aioSession;

    /**
     * 消息体
     */
    private Request message;


    public Request getMessage() {
        return message;
    }

    public void setMessage(Request message) {
        this.message = message;
    }

    public UdpAioSession<Request> getAioSession() {
        return aioSession;
    }

    public void setAioSession(UdpAioSession<Request> aioSession) {
        this.aioSession = aioSession;
    }
}
