/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StateMachineEnum.java
 * Date: 2018-03-12
 * Author: sandao
 */

package org.smartboot.socket;

/**
 * @author 三刀
 * @version V1.0.0
 */
public enum StateMachineEnum {
    /**连接已建立并构建Session对象*/
    NEW_SESSION,
    /**读通道已被关闭*/
    INPUT_SHUTDOWN,
    /**业务处理异常*/
    PROCESS_EXCEPTION,
    /**读操作异常*/
    INPUT_EXCEPTION,
    /**写操作异常*/
    OUTPUT_EXCEPTION,
    /**会话正在关闭中*/
    SESSION_CLOSING,
    /**会话关闭成功*/
    SESSION_CLOSED;
}
