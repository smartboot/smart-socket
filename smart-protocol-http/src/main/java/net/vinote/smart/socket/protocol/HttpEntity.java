package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.transport.TransportSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zhengjunwei on 2017/6/20.
 */
public class HttpEntity {
    private TransportSession<HttpEntity> session;
    private Header header = new Header();
    boolean endOfHeader = false;
    ArrayBlockingQueue<ByteBuffer> bodyStreamQueue = new ArrayBlockingQueue<ByteBuffer>(10);
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    {
        buffer.flip();
    }

    private InputStream bodyStream = new InputStream() {
        ByteBuffer currentBuffer;

        @Override
        public int read() throws IOException {
            try {
                //到达流的末尾
                if ((currentBuffer == null || !currentBuffer.hasRemaining()) && bodyStreamQueue.peek() == null && session.isEndOfStream()) {
                    return -1;
                }
                if (currentBuffer != null && currentBuffer.hasRemaining()) {
                    byte b = currentBuffer.get();
                    return b;
                }
                while (true) {
                    currentBuffer = bodyStreamQueue.poll(100, TimeUnit.MILLISECONDS);
                    if ((currentBuffer != null && currentBuffer.hasRemaining()) || session.isEndOfStream()) {
                        break;
                    }
                }
                return read();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
        }

        @Override
        public int available() throws IOException {
            if (currentBuffer != null && currentBuffer.hasRemaining()) {
                return currentBuffer.remaining();
            }
            return bodyStreamQueue.peek() == null ? 0 : bodyStreamQueue.peek().remaining();
        }
    };
    private boolean endOfBody = false;

    public HttpEntity(TransportSession<HttpEntity> session) {
        this.session = session;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public InputStream getBodyStream() {
        this.session.resumeReadAttention();
        return bodyStream;
    }


    public void appendBodyStream(ByteBuffer buffer) {
        try {
            ByteBuffer b = ByteBuffer.allocate(buffer.remaining()).put(buffer);
            b.flip();
            bodyStreamQueue.put(b);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
