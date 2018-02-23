/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DataFraming.java
 * Date: 2018-02-10
 * Author: sandao
 */

package org.smartboot.socket.http.websocket;

import org.smartboot.socket.http.HttpHeader;
import org.smartboot.socket.http.HttpRequest;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/10
 */
public class DataFraming extends HttpRequest {
    /**
     * websocket头部两个字节
     */
    private short head2Bytes;
    private State state = State.READING_FIRST;
    private long framePayloadLength;
    public DataFraming(HttpHeader header) {
        super(header);
    }


    public int getFrameFin() {
        return head2Bytes & 0x8000;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getFramePayloadLength() {
        return framePayloadLength;
    }

    public void setFramePayloadLength(long framePayloadLength) {
        this.framePayloadLength = framePayloadLength;
    }

    enum State {
        READING_FIRST,
        READING_SECOND,
        READING_SIZE,
        MASKING_KEY,
        PAYLOAD,
        CORRUPT
    }
}
