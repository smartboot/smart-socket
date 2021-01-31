/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IOUtil.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/2
 */
final class IOUtil {
    /**
     * @param channel 需要被关闭的通道
     */
    public static void close(AsynchronousSocketChannel channel) {
        if (channel == null) {
            throw new NullPointerException();
        }
        try {
            channel.shutdownInput();
        } catch (IOException ignored) {
        }
        try {
            channel.shutdownOutput();
        } catch (IOException ignored) {
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
