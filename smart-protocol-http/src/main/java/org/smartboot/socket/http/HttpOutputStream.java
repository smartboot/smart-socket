/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BodyOutputStream.java
 * Date: 2018-02-03
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.HttpHeaderNames;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class HttpOutputStream extends OutputStream {

    private AioSession aioSession;

    private HttpResponse httpResponse;

    private ByteBuffer cacheBuffer = ByteBuffer.allocate(512);
    private boolean committed = false, closed = false;

    private boolean chunked = false;

    public HttpOutputStream(AioSession aioSession, HttpResponse httpResponse) {
        this.aioSession = aioSession;
        this.httpResponse = httpResponse;
    }

    @Override
    public void write(int b) throws IOException {
        if (cacheBuffer.hasRemaining()) {
            cacheBuffer.put((byte) b);
        } else {
            if (!committed) {
                writeHead();
                committed = true;
            }
            cacheBuffer.flip();
            writeCacheBuffer(cacheBuffer);
            cacheBuffer = ByteBuffer.allocate(512);
            cacheBuffer.put((byte) b);
        }
    }

    private void writeHead() throws IOException {
        if (httpResponse.getHttpStatus() == null) {
            httpResponse.setHttpStatus(HttpStatus.OK);
        }
        ByteBuffer headBuffer = ByteBuffer.allocate(512);
        headBuffer.put(getBytes(httpResponse.getProtocol()))
                .put(Consts.SP)
                .put(getBytes(String.valueOf(httpResponse.getHttpStatus().value())))
                .put(Consts.SP)
                .put(getBytes(httpResponse.getHttpStatus().getReasonPhrase()))
                .put(Consts.CR).put(Consts.LF);

        //自动识别Transfer-Encoding
        Map<String, String> headMap = httpResponse.getHeadMap();
        if (!headMap.containsKey(HttpHeaderNames.CONTENT_LENGTH) && !headMap.containsKey(HttpHeaderNames.TRANSFER_ENCODING)
                && httpResponse.getHttpStatus() == HttpStatus.OK) {
            httpResponse.setHeader(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
            chunked = true;
        }

        for (Map.Entry<String, String> entry : httpResponse.getHeadMap().entrySet()) {
            byte[] headKey = getBytes(entry.getKey());
            byte[] headVal = getBytes(entry.getValue());

            int needLength = headKey.length + headVal.length + 3;
            if (headBuffer.remaining() < needLength) {
                headBuffer.flip();
                aioSession.write(headBuffer);
                headBuffer = ByteBuffer.allocate(needLength < 512 ? 512 : needLength);
            }
            headBuffer.put(headKey)
                    .put(Consts.COLON)
                    .put(headVal)
                    .put(Consts.CR).put(Consts.LF);
        }
        if (headBuffer.remaining() >= 2) {
            headBuffer.put(Consts.CR).put(Consts.LF);
            headBuffer.flip();
            aioSession.write(headBuffer);
        } else {
            headBuffer.flip();
            aioSession.write(headBuffer);
            aioSession.write(ByteBuffer.wrap(new byte[]{Consts.CR, Consts.LF}));
        }
    }

    @Override
    public void flush() throws IOException {
        if (!committed) {
            writeHead();
            committed = true;
        }
        cacheBuffer.flip();
        if (cacheBuffer.hasRemaining()) {
            writeCacheBuffer(cacheBuffer);
            cacheBuffer = ByteBuffer.allocate(512);
        } else {
            cacheBuffer.clear();
        }

    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        flush();
        if (chunked) {
            aioSession.write(ByteBuffer.wrap(new byte[]{'0', '\r', '\n', '\r', '\n'}));
        }
        aioSession.close(false);
        closed = true;
    }

    private byte[] getBytes(String str) {
        return str.getBytes(Consts.DEFAULT_CHARSET);
    }

    private void writeCacheBuffer(ByteBuffer cacheBuffer) throws IOException {
        if (chunked) {
            aioSession.write(ByteBuffer.wrap(getBytes(Integer.toHexString(cacheBuffer.remaining()) + "\r\n")));
            aioSession.write(cacheBuffer);
            aioSession.write(ByteBuffer.wrap(HttpProtocol.CRLF));
        } else {
            aioSession.write(cacheBuffer);
        }
    }
}
