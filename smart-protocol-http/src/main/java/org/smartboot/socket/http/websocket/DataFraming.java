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

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/10
 */
public class DataFraming extends HttpRequest {

    private State state = State.READING_FIRST;
    private boolean frameFinalFlag;
    private boolean frameMasked;
    private int frameRsv;
    private int frameOpcode;
    private int framePayloadLen1;
    private long framePayloadLength;
    private byte[] maskingKey;

    private ByteBuffer data;

    public DataFraming(HttpHeader header) {
        super(header);
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

    public boolean isFrameFinalFlag() {
        return frameFinalFlag;
    }

    public void setFrameFinalFlag(boolean frameFinalFlag) {
        this.frameFinalFlag = frameFinalFlag;
    }

    public boolean isFrameMasked() {
        return frameMasked;
    }

    public void setFrameMasked(boolean frameMasked) {
        this.frameMasked = frameMasked;
    }

    public int getFrameRsv() {
        return frameRsv;
    }

    public void setFrameRsv(int frameRsv) {
        this.frameRsv = frameRsv;
    }

    public int getFrameOpcode() {
        return frameOpcode;
    }

    public void setFrameOpcode(int frameOpcode) {
        this.frameOpcode = frameOpcode;
    }

    public int getFramePayloadLen1() {
        return framePayloadLen1;
    }

    public void setFramePayloadLen1(int framePayloadLen1) {
        this.framePayloadLen1 = framePayloadLen1;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }
}
