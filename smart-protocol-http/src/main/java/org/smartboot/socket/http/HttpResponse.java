/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpResponse.java
 * Date: 2018-02-03
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class HttpResponse {

    /**
     * http响应码
     */
    private HttpStatus httpStatus;

    private String protocol;

    private Map<String, String> headMap = new HashMap<String, String>();

    private HttpOutputStream outputStream;

    protected HttpResponse() {

    }

    public HttpResponse(AioSession<HttpRequest> session, HttpRequest request) {
        this.protocol = request.getProtocol();
        this.outputStream = new HttpOutputStream(session, this, request);
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    Map<String, String> getHeadMap() {
        return headMap;
    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
    }

    public String getHeader(String name) {
        return headMap.get(name);
    }

    String getProtocol() {
        return protocol;
    }

    public void write(ByteBuffer buffer) throws IOException {
        outputStream.write(buffer);
    }
}
