package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.AbstractMap;

class WriteCompletionHandler<T> implements CompletionHandler<Integer, AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer>> {
    private static final Logger logger = LogManager.getLogger(WriteCompletionHandler.class);

    @Override
    public void completed(Integer result, AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer> attachment) {
        AioSession<T> aioSession = attachment.getKey();
        ByteBuffer writeBuffer = attachment.getValue();
        //服务端Session才具备流控功能
        aioSession.tryReleaseFlowLimit();

        if (writeBuffer.hasRemaining()) {
            aioSession.channel.write(writeBuffer, attachment, this);
            return;
        }
        if (aioSession.writeCacheQueue.isEmpty()) {
            aioSession.semaphore.release();
            if (aioSession.isInvalid()) {
                aioSession.close();
                return;
            }
            if (!aioSession.writeCacheQueue.isEmpty()) {
                aioSession.channelWriteProcess(true);
            }
        } else {
            aioSession.channelWriteProcess(false);
        }

    }

    @Override
    public void failed(Throwable exc, AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer> attachment) {
        logger.warn(exc.getMessage());
        attachment.getKey().close();
    }
}