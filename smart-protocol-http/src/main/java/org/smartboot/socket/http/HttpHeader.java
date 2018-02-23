/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpHeader.java
 * Date: 2018-02-15
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.utils.HttpHeaderConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * Http协议消息头部分
 *
 * @author 三刀
 * @version V1.0 , 2018/2/15
 */
public class HttpHeader {

    private MethodEnum method;
    private String originalUri;
    private String protocol;

    private String scheme;

    private Map<String, String> headerMap = new HashMap<String, String>();

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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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
