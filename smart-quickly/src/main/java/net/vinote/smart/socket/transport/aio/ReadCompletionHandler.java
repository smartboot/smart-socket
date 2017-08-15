package net.vinote.smart.socket.transport.aio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

class ReadCompletionHandler implements CompletionHandler<Integer, Object> {
    AsynchronousSocketChannel channel;
    AioSession session;

    public ReadCompletionHandler(AsynchronousSocketChannel channel, AioSession session) {
        this.channel = channel;
        this.session = session;
    }

    @Override
    public void completed(Integer result, Object attachment) {
        if (result == -1) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        ByteBuffer readBuffer = (ByteBuffer) attachment;
        readBuffer.flip();

        session.read(readBuffer);
        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {// 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }
        if(session.isServer) {
            if (session.writeCacheQueue.size() < AioSession.FLOW_LIMIT_LINE) {
                channel.read(readBuffer, attachment, this);
            } else {
//            System.err.println("流控");
                session.flowLimit.set(true);
            }
        }else{
            channel.read(readBuffer, attachment, this);
        }
    }

    @Override
    public void failed(Throwable exc, Object attachment) {
        exc.printStackTrace();
    }
}