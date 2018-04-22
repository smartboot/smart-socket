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
import org.smartboot.socket.http.HttpDecodeUnit;
import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.utils.EmptyInputStream;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public class Http11ContentDecoder implements Protocol<HttpRequest> {

    private void decodeBodyForm(HttpDecodeUnit unit) {
        ByteBuffer buffer = unit.getFormDecoder().getBuffer();
        String[] paramArray = StringUtils.split(new String(buffer.array(), buffer.position(), buffer.remaining()), "&");
        for (int i = 0; i < paramArray.length; i++) {
            ((Http11Request) unit.getEntity()).setParam(StringUtils.substringBefore(paramArray[i], "=").trim(), StringUtils.substringAfter(paramArray[i], "=").trim());
        }
    }

    @Override
    public HttpRequest decode(ByteBuffer buffer, AioSession<HttpRequest> session, boolean eof) {
        HttpDecodeUnit decodeUnit = (HttpDecodeUnit) session.getAttachment();
        if (decodeUnit == null) {
            throw new RuntimeException("decodeUnit is null");
        }
        Http11Request httpRequest = (Http11Request) decodeUnit.getEntity();
        if (httpRequest == null) {
            throw new RuntimeException("request is null");
        }
        //非Post请求，解码完成
        if (httpRequest.getHeader().getMethod() != MethodEnum.POST) {
            return httpRequest;
        }

        FixedLengthFrameDecoder formDecoder = decodeUnit.getFormDecoder();
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
                decodeUnit.setFormDecoder(formDecoder);
                httpRequest.setInputStream(new EmptyInputStream());
            }
        }
        if (formDecoder.decode(buffer)) {
            decodeBodyForm(decodeUnit);
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
