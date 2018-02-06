/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpV2Entity.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.socket.transport.AioSession;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 三刀 on 2017/6/20.
 */
public class HttpRequest {


    private InputStream inputStream = null;
    private int contentLength = -1;
    private String method, uri, protocol, contentType;
    private String scheme;//HTTP\HTTPS...
    private Map<String, String> headMap = new HashMap<String, String>();
    private Map<String, String> paramMap = new HashMap<String, String>();

    public HttpRequest(AioSession<HttpRequest> session) {
    }


    public InputStream getInputStream() {
        return inputStream;
    }

    void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
        if (StringUtils.equals(name, HttpHeaderNames.CONTENT_LENGTH)) {
            contentLength = NumberUtils.toInt(value, -1);
        } else if (StringUtils.startsWith(name, HttpHeaderNames.CONTENT_TYPE)) {
            contentType = value;
        }
    }

    public String getHeader(String key) {
        return headMap.get(key);
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }


    public void setParam(String key, String val) {
        this.paramMap.put(key, val);
    }

}
