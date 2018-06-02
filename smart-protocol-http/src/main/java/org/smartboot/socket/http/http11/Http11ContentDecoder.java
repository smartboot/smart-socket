/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Http11ContentDecoder.java
 * Date: 2018-02-16
 * Author: sandao
 */

package org.smartboot.socket.http.http11;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.utils.EmptyInputStream;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public class Http11ContentDecoder implements Protocol<HttpRequest> {
    private static final AttachKey<Http11Request> ENTITY = AttachKey.valueOf("entity");
    private static final AttachKey<FixedLengthFrameDecoder> FIXED_LENGTH_FRAME_DECODER_ATTACH_KEY = AttachKey.valueOf("fixLengthFrameDecoder");

    private void decodeBodyForm(FixedLengthFrameDecoder decoder, Http11Request request) {
        ByteBuffer buffer = decoder.getBuffer();
        String[] paramArray = StringUtils.split(new String(buffer.array(), buffer.position(), buffer.remaining()), "&");
        for (int i = 0; i < paramArray.length; i++) {
            request.setParam(StringUtils.substringBefore(paramArray[i], "=").trim(), StringUtils.substringAfter(paramArray[i], "=").trim());
        }
    }

    @Override
    public HttpRequest decode(ByteBuffer buffer, AioSession<HttpRequest> session, boolean eof) {
        Attachment attachment = session.getAttachment();
        if (attachment == null) {
            throw new RuntimeException("decodeUnit is null");
        }
        Http11Request httpRequest = attachment.get(ENTITY);
        if (httpRequest == null) {
            throw new RuntimeException("request is null");
        }
        //非Post请求，解码完成
        if (httpRequest.getHeader().getMethod() != MethodEnum.POST) {
            return httpRequest;
        }

        FixedLengthFrameDecoder formDecoder = attachment.get(FIXED_LENGTH_FRAME_DECODER_ATTACH_KEY);
        if (formDecoder == null) {
            if (StringUtils.startsWith(httpRequest.getHeader().getHeader(HttpHeaderConstant.Names.CONTENT_TYPE), HttpHeaderConstant.Values.MULTIPART_FORM_DATA)) {
                int contentLength = NumberUtils.toInt(httpRequest.getHeader().getHeader(HttpHeaderConstant.Names.CONTENT_LENGTH), -1);
                try {
                    httpRequest.setInputStream(session.getInputStream(contentLength));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return httpRequest;
            } else {
                int contentLength = NumberUtils.toInt(httpRequest.getHeader().getHeader(HttpHeaderConstant.Names.CONTENT_LENGTH), -1);
                formDecoder = new FixedLengthFrameDecoder(contentLength);
                attachment.put(FIXED_LENGTH_FRAME_DECODER_ATTACH_KEY, formDecoder);
                httpRequest.setInputStream(new EmptyInputStream());
            }
        }
        if (formDecoder.decode(buffer)) {
            decodeBodyForm(formDecoder, httpRequest);
            attachment.remove(FIXED_LENGTH_FRAME_DECODER_ATTACH_KEY);
            return httpRequest;
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer encode(HttpRequest msg, AioSession<HttpRequest> session) {
        return null;
    }
}
