/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HandshakeCompletion.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/2
 */
class HandshakeCompletion implements CompletionHandler<Integer, HandshakeModel> {
    private static final Logger logger = LoggerFactory.getLogger(HandshakeCompletion.class);
    private SslService sslService;

    public HandshakeCompletion(SslService sslService) {
        this.sslService = sslService;
    }

    @Override
    public void completed(Integer result, HandshakeModel attachment) {
        if (result == -1) {
            attachment.setEof(true);
        }
        synchronized (attachment) {
            sslService.doHandshake(attachment);
        }
    }

    @Override
    public void failed(Throwable exc, HandshakeModel attachment) {
        try {
            attachment.getSocketChannel().close();
            attachment.getSslEngine().closeOutbound();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.warn("handshake exception", exc);
    }
}
