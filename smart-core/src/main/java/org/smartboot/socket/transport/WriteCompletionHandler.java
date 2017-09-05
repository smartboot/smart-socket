package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import static java.util.AbstractMap.SimpleEntry;

class WriteCompletionHandler<T> implements CompletionHandler<Integer, SimpleEntry<AioSession<T>, ByteBuffer>> {
    private static final Logger logger = LogManager.getLogger(WriteCompletionHandler.class);

    @Override
    public void completed(Integer result, SimpleEntry<AioSession<T>, ByteBuffer> attachment) {
        AioSession<T> aioSession = attachment.getKey();
        ByteBuffer writeBuffer = attachment.getValue();
        //服务端Session才具备流控功能
        aioSession.tryReleaseFlowLimit();
        aioSession.writeToChannel(writeBuffer.hasRemaining() ? writeBuffer : null);
    }

    @Override
    public void failed(Throwable exc, SimpleEntry<AioSession<T>, ByteBuffer> attachment) {
        logger.warn(exc.getMessage());
        attachment.getKey().close();
    }
}