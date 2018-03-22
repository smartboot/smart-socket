/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpHeader.java
 * Date: 2018-02-15
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.MethodEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Http协议消息头部分
 *
 * @author 三刀
 * @version V1.0 , 2018/2/15
 */
public class HttpHeader {

    Map<String, String> headerMap = new HashMap<String, String>();
    private MethodEnum method;
    private String originalUri;
    private String httpVersion;

    public MethodEnum getMethod() {
        return method;
    }

    public void setMethod(MethodEnum method) {
        this.method = method;
    }

    public String getOriginalUri() {
        return originalUri;
    }

    public void setOriginalUri(String originalUri) {
        this.originalUri = originalUri;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setHeader(String name, String value) {
        headerMap.put(name, value);
//        if (StringUtils.equals(name, HttpHeaderConstant.Names.CONTENT_LENGTH)) {
//            contentLength = NumberUtils.toInt(value, -1);
//        } else if (StringUtils.startsWith(name, HttpHeaderConstant.Names.CONTENT_TYPE)) {
//            contentType = value;
//        }
    }

    public String getHeader(String key) {
        return headerMap.get(key);
    }
}
