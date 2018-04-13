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

    SPACES_BEFORE_URI,


    AFTER_SLASH_IN_URI,

    SCHEMA,

    SCHEMA_SLASH,

    SCHEMA_SLASH_SLASH,

    HOST_START,

    HOST_IP_LITERAL,

    HOST,
    HOST_END,
    PORT,
    HOST_HTTP_09,
    CHECK_URI_HTTP_09,
    HTTP_H,
    HTTP_HT,
    HTTP_HTT,
    HTTP_HTTP,
    HTTP_VERSION,

    /**
     * 请求头部
     */
    HEAD_LINE_KEY,

    HEAD_LINE_VALUE,

    HEAD_LINE_VALUE_BUFFER,

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
