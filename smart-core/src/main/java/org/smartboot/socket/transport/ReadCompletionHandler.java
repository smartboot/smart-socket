package org.smartboot.socket.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;

class ReadCompletionHandler implements CompletionHandler<Integer, AioSession> {
    private Logger logger = LogManager.getLogger(ReadCompletionHandler.class);


    @Override
    public void completed(Integer result, AioSession aioSession) {
        if (result == -1) {
            logger.debug("read end:" + aioSession);
            aioSession.close(false);
            return;
        }

        ByteBuffer readBuffer = aioSession.getReadBuffer();
        readBuffer.flip();
        aioSession.read(readBuffer);

        //会话已不可用,终止读
        if (aioSession.getStatus() != IoSessionStatusEnum.ENABLED) {
            return;
        }
        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {// 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }

        //是否触发流控
        if (aioSession.serverFlowLimit != null && aioSession.writeCacheQueue.size() > aioSession.FLOW_LIMIT_LINE) {
            aioSession.serverFlowLimit.set(true);
        } else {
            aioSession.registerReadHandler();
        }
    }

    @Override
    public void failed(Throwable exc, AioSession aioSession) {
        if (exc instanceof AsynchronousCloseException) {
            logger.debug(exc);
        } else {
            exc.printStackTrace();
        }
        aioSession.close();

    }
}