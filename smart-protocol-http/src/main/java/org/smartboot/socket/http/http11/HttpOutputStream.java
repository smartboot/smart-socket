/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpOutputStream.java
 * Date: 2018-02-17
 * Author: sandao
 */

package org.smartboot.socket.http.http11;

import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
final class HttpOutputStream extends OutputStream {

    private AioSession aioSession;

    private DefaultHttpResponse response;

    private ByteBuffer cacheBuffer = ByteBuffer.allocate(512);
    private boolean committed = false, closed = false;

    private boolean chunked = false;
    private Http11Request request;

    public HttpOutputStream(AioSession aioSession, DefaultHttpResponse response, Http11Request request) {
        this.aioSession = aioSession;
        this.response = response;
        this.request = request;
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

    public void write(ByteBuffer buffer) throws IOException {
        flush();
        writeCacheBuffer(buffer);
    }

    private void writeHead() throws IOException {
        Http11HandleGroup.group().getLastHandle().doHandle(request, new NoneOutputHttpResponseWrap(response));//防止在handle中调用outputStream操作
        chunked = StringUtils.equals(HttpHeaderConstant.Values.CHUNKED, response.getHeader(HttpHeaderConstant.Names.TRANSFER_ENCODING));

        ByteBuffer headBuffer = ByteBuffer.allocate(512);
        headBuffer.put(getBytes(request.getHeader().getProtocol()))
                .put(Consts.SP)
                .put(getBytes(String.valueOf(response.getHttpStatus().value())))
                .put(Consts.SP)
                .put(getBytes(response.getHttpStatus().getReasonPhrase()))
                .put(Consts.CR).put(Consts.LF);


        for (Map.Entry<String, String> entry : response.getHeadMap().entrySet()) {
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
            aioSession.write(ByteBuffer.wrap(new byte[]{'0', Consts.CR, Consts.LF, Consts.CR, Consts.LF}));
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
            aioSession.write(ByteBuffer.wrap(Consts.CRLF));
        } else {
            aioSession.write(cacheBuffer);
        }
    }

}
