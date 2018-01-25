/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StateMachineEnum.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket;

/**
 * @author 三刀
 * @version V1.0 , 2017/9/12
 */
public enum StateMachineEnum {
    /**连接已建立并构建Session对象*/
    NEW_SESSION,
    /**读通道已被关闭*/
    INPUT_SHUTDOWN,
    /**读操作异常*/
    INPUT_EXCEPTION,
    /**写操作异常*/
    OUTPUT_EXCEPTION,
    /**会话正在关闭中*/
    SESSION_CLOSING,
    /**会话关闭成功*/
    SESSION_CLOSED;
}
