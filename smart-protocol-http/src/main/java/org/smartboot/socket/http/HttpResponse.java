/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpResponse.java
 * Date: 2018-02-03
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.HttpStatus;

import java.io.OutputStream;
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

    private OutputStream outputStream;


    public HttpResponse(String protocol) {
        this.protocol = protocol;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
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

    String getProtocol() {
        return protocol;
    }
}
