/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpResponWrap.java
 * Date: 2018-02-08
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/8
 */
class NoneOutputHttpResponWrap extends HttpResponse {
    private HttpResponse response;

    public NoneOutputHttpResponWrap(HttpResponse response) {
        this.response = response;
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpStatus getHttpStatus() {
        return response.getHttpStatus();
    }

    @Override
    public void setHttpStatus(HttpStatus httpStatus) {
        response.setHttpStatus(httpStatus);
    }

    @Override
    Map<String, String> getHeadMap() {
        return response.getHeadMap();
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public String getHeader(String name) {
        return response.getHeader(name);
    }

    @Override
    String getProtocol() {
        return response.getProtocol();
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        throw new UnsupportedOperationException();
    }
}
