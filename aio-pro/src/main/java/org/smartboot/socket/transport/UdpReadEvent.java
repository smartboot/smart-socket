/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: UdpReadEvent.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

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
