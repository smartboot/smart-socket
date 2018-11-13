package org.smartboot.socket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.buffer.BufferPagePool;
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

class BufferOutputStream extends OutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferOutputStream.class);
    /**
     * 输出缓存块大小
     */
    private static final int WRITE_CHUNK_SIZE = IoServerConfig.getIntProperty(IoServerConfig.Property.SESSION_WRITE_CHUNK_SIZE, 1024);
    LinkedBlockingQueue<VirtualBuffer> bufList = new LinkedBlockingQueue<>();
    private VirtualBuffer writeInBuf;
    private BufferPagePool bufferPagePool;
    private boolean closed = false;
    private Function<? super BlockingQueue<VirtualBuffer>, Void> function;

    public BufferOutputStream(BufferPagePool bufferPagePool, Function<? super BlockingQueue<VirtualBuffer>, Void> flushFunction) {
        this.bufferPagePool = bufferPagePool;
        this.function = flushFunction;
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("OutputStream has closed");
        }
//            System.out.println("1");
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        if (writeInBuf == null) {
            writeInBuf = bufferPagePool.allocateBufferPage().allocate(WRITE_CHUNK_SIZE);
//            LOGGER.info("OutputStream:{} 申请writeInBuf:{}",this.hashCode(),writeInBuf.hashCode()+""+writeInBuf);
        }

        do {
            ByteBuffer writeBuffer = writeInBuf.buffer();
            int minSize = Math.min(writeBuffer.remaining(), len - off);
            if (minSize == 0 || closed) {
//                LOGGER.info("OutputStream:{} 回收writeInBuf:{}",this.hashCode(),writeBuffer.hashCode()+""+writeBuffer);
                writeInBuf.clean();
                throw new IOException("writeBuffer.remaining:" + writeBuffer.remaining() + " closed:" + closed);
            }
            writeBuffer.put(b, off, minSize);
            off += minSize;
            if (!writeBuffer.hasRemaining()) {
                writeBuffer.flip();
                bufList.add(writeInBuf);
                writeInBuf = null;
                function.apply(bufList);
                if (off < len) {
                    writeInBuf = bufferPagePool.allocateBufferPage().allocate(WRITE_CHUNK_SIZE);
//                    LOGGER.info("OutputStream:{} 申请writeInBuf:{}",this.hashCode(),writeInBuf);
                    if (closed) {
                        LOGGER.info("closed");
//                        LOGGER.info("OutputStream:{} 回收writeInBuf:{}",this.hashCode(),writeBuffer.hashCode()+""+writeBuffer);
                        writeInBuf.clean();
                    }
                }
            }
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
        closed = true;

        if (bufList != null) {
            VirtualBuffer byteBuf = null;
            while ((byteBuf = bufList.poll()) != null) {
//                LOGGER.info("clean" + byteBuf);
//                LOGGER.info("OutputStream:{} 回收writeInBuf:{}",this.hashCode(),byteBuf.hashCode()+""+byteBuf);
                byteBuf.clean();
            }
        }
        if (writeInBuf != null) {
//            LOGGER.info("OutputStream:{} 回收writeInBuf:{}",this.hashCode(),writeInBuf.hashCode()+""+writeInBuf);
            writeInBuf.clean();
            LOGGER.info("clean" + writeInBuf);
        }
    }

    public synchronized boolean hasData() {
        return bufList.size() > 0 || (writeInBuf != null && writeInBuf.buffer().position() > 0);
    }
}