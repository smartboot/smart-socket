package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author 三刀
 * @version V1.0 , 2018/11/8
 */

public class BufferOutputStream extends OutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferOutputStream.class);
    /**
     * 输出缓存块大小
     */
    private static final int WRITE_CHUNK_SIZE = IoServerConfig.getIntProperty(IoServerConfig.Property.SESSION_WRITE_CHUNK_SIZE, 1024);
    LinkedBlockingQueue<VirtualBuffer> bufList = new LinkedBlockingQueue<>();
    private VirtualBuffer writeInBuf;
    private BufferPage bufferPage;
    private boolean closed = false;
    private Function<? super BlockingQueue<VirtualBuffer>, Void> function;

    BufferOutputStream(BufferPage bufferPage, Function<? super BlockingQueue<VirtualBuffer>, Void> flushFunction) {
        this.bufferPage = bufferPage;
        this.function = flushFunction;
    }

    @Override
    public synchronized void write(int b) {
        if (writeInBuf == null) {
            writeInBuf = bufferPage.allocate(WRITE_CHUNK_SIZE);
        }
        writeInBuf.buffer().put((byte) b);
        if (writeInBuf.buffer().hasRemaining()) {
            return;
        }
        writeInBuf.buffer().flip();
        bufList.add(writeInBuf);
        writeInBuf = null;
        function.apply(bufList);
    }

    public synchronized void writeInt(int v) {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("OutputStream has closed");
        }
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }


        do {
            if (writeInBuf == null) {
                writeInBuf = bufferPage.allocate(WRITE_CHUNK_SIZE);
            }
            ByteBuffer writeBuffer = writeInBuf.buffer();
            int minSize = Math.min(writeBuffer.remaining(), len - off);
            if (minSize == 0 || closed) {
                writeInBuf.clean();
                throw new IOException("writeBuffer.remaining:" + writeBuffer.remaining() + " closed:" + closed);
            }
            writeBuffer.put(b, off, minSize);
            off += minSize;
            if (writeBuffer.hasRemaining()) {
                continue;
            }
            //缓冲区已满
            writeBuffer.flip();
            bufList.add(writeInBuf);
            writeInBuf = null;
            function.apply(bufList);
        } while (off < len);
    }


    @Override
    public synchronized void flush() {
        if (closed) {
            throw new RuntimeException("OutputStream has closed");
        }
        if (writeInBuf != null && writeInBuf.buffer().position() > 0) {
            writeInBuf.buffer().flip();
            bufList.add(writeInBuf);
            writeInBuf = null;
        }
        function.apply(bufList);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            throw new IOException("OutputStream has closed");
        }
        flush();
        closed = true;

        if (bufList != null) {
            VirtualBuffer byteBuf = null;
            while ((byteBuf = bufList.poll()) != null) {
                byteBuf.clean();
            }
        }
        if (writeInBuf != null) {
            writeInBuf.clean();
            LOGGER.info("clean" + writeInBuf);
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean hasData() {
        return bufList.size() > 0 || (writeInBuf != null && writeInBuf.buffer().position() > 0);
    }
}