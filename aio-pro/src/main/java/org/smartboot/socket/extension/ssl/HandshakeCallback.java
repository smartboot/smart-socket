/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HandshakeCallback.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/2
 */
interface HandshakeCallback {
    /**
     * 握手回调
     */
    void callback();
}
