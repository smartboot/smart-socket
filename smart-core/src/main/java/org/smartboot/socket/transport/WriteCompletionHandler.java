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
        if (aioSession.serverFlowLimit != null && aioSession.writeCacheQueue.size() < aioSession.RELEASE_LINE && aioSession.serverFlowLimit.get()) {
            aioSession.serverFlowLimit.set(false);
            aioSession.registerReadHandler();
        }
        if (writeBuffer.hasRemaining()) {
            //复用输出流
            int avail = writeBuffer.capacity() - writeBuffer.remaining();
            ByteBuffer nextByteBuffer = aioSession.writeCacheQueue.peek();
            if (nextByteBuffer != null && nextByteBuffer.remaining() <= avail) {
                writeBuffer.compact();
                int pollSize = 0;
                while ((nextByteBuffer = aioSession.writeCacheQueue.peek()) != null && nextByteBuffer.remaining() <= writeBuffer.remaining()) {
                    pollSize += nextByteBuffer.remaining();
                    writeBuffer.put(aioSession.writeCacheQueue.poll());
                }
                aioSession.writeCacheSize.getAndAdd(-pollSize);
                writeBuffer.flip();
            }

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
                aioSession.trigeWrite(true);
            }
        } else {
            aioSession.trigeWrite(false);
        }

    }

    @Override
    public void failed(Throwable exc, AbstractMap.SimpleEntry<AioSession<T>, ByteBuffer> attachment) {
        logger.warn(exc.getMessage());
        attachment.getKey().close();
    }
}