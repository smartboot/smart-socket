package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.transport.TransportSession;
import org.apache.commons.lang.math.NumberUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zhengjunwei on 2017/6/20.
 */
public class HttpEntity {
    public static final String AUTHORIZATION = "Authorization";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String DATE = "Date";
    public static final String ETAG = "ETag";
    public static final String EXPIRES = "Expires";
    public static final String HOST = "Host";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String RANGE = "Range";
    public static final String LOCATION = "Location";
    public static final String CONNECTION = "Connection";
    private TransportSession<HttpEntity> session;
    /**
     * 请求行
     */
    private RequestLine requestLine;
    private Map<String, String> headMap = new HashMap<String, String>();

    boolean endOfHeader = false;
    ArrayBlockingQueue<ByteBuffer> bodyStreamQueue = new ArrayBlockingQueue<ByteBuffer>(10);
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    {
        buffer.flip();
    }

    private boolean endOfBody = false;
    private AtomicInteger readBodyLength = new AtomicInteger(0);
    private int contentLength = -1;
    private int chunked=-1;
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
            if(contentLength>0){
                return contentLength-readBodyLength.get();
            }
            if(chunked>0){
                return chunked-readBodyLength.get();
            }
            if (currentBuffer != null && currentBuffer.hasRemaining()) {
                return currentBuffer.remaining();
            }
            return bodyStreamQueue.peek() == null ? 0 : bodyStreamQueue.peek().remaining();
        }
    };


    public HttpEntity(TransportSession<HttpEntity> session) {
        this.session = session;
    }

    public void setHeader(String name, String value) {
        if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
            contentLength = NumberUtils.toInt(value);
        }
        headMap.put(name, value);
    }

    public void setRequestLine(RequestLine requestLine) {
        this.requestLine = requestLine;
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public Map<String, String> getHeadMap() {
        return headMap;
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

    public void appendBodyStream(byte[] data) {
        try {
            ByteBuffer b = ByteBuffer.allocate(data.length).put(buffer);
            b.flip();
            bodyStreamQueue.put(b);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public AtomicInteger getReadBodyLength() {
        return readBodyLength;
    }

    public void setReadBodyLength(AtomicInteger readBodyLength) {
        this.readBodyLength = readBodyLength;
    }

    public int getChunked() {
        return chunked;
    }

    public void setChunked(int chunked) {
        this.chunked = chunked;
    }
}
