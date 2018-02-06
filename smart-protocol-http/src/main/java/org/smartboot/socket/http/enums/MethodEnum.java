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

    MethodEnum(String method) {
        this.method = method;
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
