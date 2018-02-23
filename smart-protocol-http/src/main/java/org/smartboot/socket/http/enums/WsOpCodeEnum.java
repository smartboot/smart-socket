/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WsOpCodeEnum.java
 * Date: 2018-02-10
 * Author: sandao
 */

package org.smartboot.socket.http.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/10
 * @since 1.3.2
 */
public enum WsOpCodeEnum {
    /**
     * 继续帧
     */
    CONTINUE((byte) 0x0),
    /**
     * 文本帧
     */
    TEXT((byte) 0x01),
    /**
     * 二进制帧
     */
    BIN((byte) 0x02),

    /**
     * 预留帧
     */
    RESERVE_3((byte) 0x03),
    /**
     * 预留帧
     */
    RESERVE_4((byte) 0x04),
    /**
     * 预留帧
     */
    RESERVE_5((byte) 0x05),
    /**
     * 预留帧
     */
    RESERVE_6((byte) 0x06),
    /**
     * 预留帧
     */
    RESERVE_7((byte) 0x07),
    /**
     * 关闭帧
     */
    CLOSE((byte) 0x08),
    /**
     * ping
     */
    PING((byte) 0x09),
    /**
     * pong
     */
    PONG((byte) 0xA),
    /**
     * 预留帧
     */
    RESERVE_B((byte) 0xB),
    /**
     * 预留帧
     */
    RESERVE_C((byte) 0xC),
    /**
     * 预留帧
     */
    RESERVE_D((byte) 0xD), /**
     * 预留帧
     */
    RESERVE_E((byte) 0xE),
    /**
     * 预留帧
     */
    RESERVE_F((byte) 0xF),;

    private byte code;

    WsOpCodeEnum(byte code) {
        this.code = code;
    }
}
