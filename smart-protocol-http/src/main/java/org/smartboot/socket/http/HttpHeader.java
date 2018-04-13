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
    Map<String, byte[]> b_headerMap = new HashMap<String, byte[]>();
    private MethodEnum method;
    private byte[] b_method;
    private byte[] b_schema;
    private byte[] b_host;
    private byte[] b_port;
    private byte[] b_uri;
    private byte[] b_http_version;
    private String originalUri;
    private byte[] versionBytes;
    private String httpVersion;

    public MethodEnum getMethod() {
        return method;
    }

    public void setMethod(MethodEnum method) {
        this.method = method;
    }

    public void setB_http_version(byte[] b_http_version) {
        this.b_http_version = b_http_version;
    }

    public void setB_uri(byte[] b_uri) {
        this.b_uri = b_uri;
    }

    public void setB_method(byte[] b_method) {
        this.b_method = b_method;
    }

    public void setB_schema(byte[] b_schema) {
        this.b_schema = b_schema;
    }

    public void setB_port(byte[] b_port) {
        this.b_port = b_port;
    }

    public void setB_host(byte[] b_host) {
        this.b_host = b_host;
    }

    public String getOriginalUri() {
        if (originalUri == null) {
            originalUri = new String(b_uri);
        }
        return originalUri;
    }

    public void setOriginalUri(String originalUri) {
        this.originalUri = originalUri;
    }

    public String getHttpVersion() {
        if (httpVersion == null) {
            httpVersion = new String(b_http_version);
        }
        return "HTTP/" + httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    void setVersionBytes(byte[] versionBytes) {
        this.versionBytes = versionBytes;
    }

    public void setHeader(String name, byte[] val) {
        b_headerMap.put(name, val);
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
        String val = headerMap.get(key);
        if (val != null) {
            return val;
        }
        byte[] b = b_headerMap.remove(key);
        if (b == null) {
            return null;
        }
        val = new String(b);
        headerMap.put(key, val);
        return val;
    }
}
