package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/2
 */
final class IOUtil {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    /**
     * @param channel 需要被关闭的通道
     */
    public static void close(AsynchronousSocketChannel channel) {
        if (channel == null) {
            throw new NullPointerException();
        }
        try {
            channel.shutdownInput();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            channel.shutdownOutput();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.debug("close channel exception", e);
        }
    }
}
