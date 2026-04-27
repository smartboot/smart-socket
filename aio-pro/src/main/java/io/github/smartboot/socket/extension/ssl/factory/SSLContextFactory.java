/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLContextFactory.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/20
 */
public interface SSLContextFactory {
    SSLContext create() throws Exception;

    void initSSLEngine(AsynchronousSocketChannel channel, SSLEngine sslEngine);
}
