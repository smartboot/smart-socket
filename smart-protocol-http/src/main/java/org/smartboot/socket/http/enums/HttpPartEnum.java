/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpPartEnum.java
 * Date: 2018-02-17
 * Author: sandao
 */

package org.smartboot.socket.http.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public enum HttpPartEnum {
    /**
     * 请求行:Method
     */
    REQUEST_LINE_METHOD,

    /**
     * 请求行:Request_URL
     */
    REQUEST_LINE_URL,

    /**
     * 请求行:HTTP_VERSION
     */
    REQUEST_LINE_VERSION,

    /**
     * 请求头部
     */
    HEAD_LINE_KEY,

    HEAD_LINE_VALUE,

    HEAD_END_CHECK,
    /**
     * 空行
     */
//    HEAD_END_LINE,
    /**
     * 请求体
     */
    CONTENT,

    /**
     * 结束
     */
    END;
}
