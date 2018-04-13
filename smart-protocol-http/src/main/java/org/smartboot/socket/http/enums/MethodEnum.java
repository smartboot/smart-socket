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
        switch (length) {
            case 3:
                if (isMatch(GET, bytes, index)) {
                    return GET;
                }
                if (isMatch(PUT, bytes, index)) {
                    return PUT;
                }
                break;
            case 4:
                if (isMatch(POST, bytes, index)) {
                    return POST;
                }
                if (isMatch(HEAD, bytes, index)) {
                    return HEAD;
                }
                break;
            case 5:
                if (isMatch(TRACE, bytes, index)) {
                    return TRACE;
                }
                break;
            case 6:
                if (isMatch(DELETE, bytes, index)) {
                    return DELETE;
                }
                break;
            case 7:
                if (isMatch(OPTIONS, bytes, index)) {
                    return OPTIONS;
                }
                if (isMatch(CONNECT, bytes, index)) {
                    return CONNECT;
                }
                break;
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

    private static boolean isMatch(MethodEnum methodEnum, byte[] bytes, int index) {
        for (int i = 0; i < methodEnum.bytes.length; i++) {
            if (methodEnum.bytes[i] != bytes[index + i]) {
                return false;
            }
        }
        return true;
    }

    public String getMethod() {
        return method;
    }
}
