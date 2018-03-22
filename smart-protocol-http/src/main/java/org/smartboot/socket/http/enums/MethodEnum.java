/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MethodEnum.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.enums;

import org.apache.commons.lang.StringUtils;

/**
 * Http支持的Method
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public enum MethodEnum {
    OPTIONS("OPTIONS"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    TRACE("TRACE"),
    CONNECT("CONNECT");

    private String method;
    private byte[] bytes;

    MethodEnum(String method) {
        this.method = method;
        this.bytes = method.getBytes();
    }

    public static MethodEnum getByMethod(byte[] bytes, int index, int length) {
        if (bytes == null || index < 0 || index >= bytes.length || length + index > bytes.length) {
            return null;
        }
        for (MethodEnum methodEnum : values()) {
            if (methodEnum.bytes.length != length) {
                continue;
            }
            boolean mark = true;
            for (int i = 0; i < methodEnum.bytes.length; i++) {
                if (methodEnum.bytes[i] != bytes[index + i]) {
                    mark = false;
                    break;
                }
            }
            if (mark) {
                return methodEnum;
            }
        }
        return null;
    }

    public static MethodEnum getByMethod(String method) {
        if (method == null) {
            return null;
        }
        for (MethodEnum methodEnum : values()) {
            if (StringUtils.equals(method, methodEnum.method)) {
                return methodEnum;
            }
        }
        return null;
    }

    public String getMethod() {
        return method;
    }
}
