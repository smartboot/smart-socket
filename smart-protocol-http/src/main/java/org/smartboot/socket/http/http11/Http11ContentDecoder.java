/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Http11ContentDecoder.java
 * Date: 2018-02-16
 * Author: sandao
 */

package org.smartboot.socket.http.http11;

import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.http.HttpDecodeUnit;
import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.enums.HttpPartEnum;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public class Http11ContentDecoder implements Protocol<HttpRequest> {

    private void decodeBodyForm(HttpDecodeUnit unit) {
        ByteBuffer buffer = unit.getFormBodyDecoder().getBuffer();
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
        HttpRequest httpRequest = decodeUnit.getEntity();
        if (httpRequest == null) {
            throw new RuntimeException("request is null");
        }
        switch (decodeUnit.getBodyTypeEnum()) {
            case FORM:
                if (decodeUnit.getFormBodyDecoder().decode(buffer)) {
                    decodeBodyForm(decodeUnit);
                    decodeUnit.setDecodePartEnum(HttpPartEnum.END);
                    decodeUnit.setPartFinished(true);
                }
                break;
            case STREAM:
                if (decodeUnit.getStreamBodyDecoder().decode(buffer)) {
                    decodeUnit.setDecodePartEnum(HttpPartEnum.END);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return null;
    }

    @Override
    public ByteBuffer encode(HttpRequest msg, AioSession<HttpRequest> session) {
        return null;
    }
}
