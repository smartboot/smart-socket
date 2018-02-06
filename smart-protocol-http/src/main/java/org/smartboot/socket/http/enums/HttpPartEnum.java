/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpPartEnum.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.enums;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/30
 */
public enum HttpPartEnum {
    /**
     * 请求行
     */
    REQUEST_LINE,
    /**
     * 请求头部
     */
    HEAD_LINE,
    /**
     * 空行
     */
    HEAD_END_LINE,
    /**
     * 请求体
     */
    BODY,
    /**
     * 结束
     */
    END;
}
