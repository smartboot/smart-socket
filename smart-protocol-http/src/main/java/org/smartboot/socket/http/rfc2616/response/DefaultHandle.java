/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DefaultHandle.java
 * Date: 2018-02-08
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616.response;

import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.rfc2616.HttpHandle;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.HttpHeaderNames;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/8
 */
public class DefaultHandle extends HttpHandle {
    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException {
        if (response.getHttpStatus() == null) {
            response.setHttpStatus(HttpStatus.OK);
        }
        if (response.getHeader(HttpHeaderNames.CONTENT_LENGTH) == null && response.getHeader(HttpHeaderNames.TRANSFER_ENCODING) == null
                && response.getHttpStatus() == HttpStatus.OK) {
            response.setHeader(HttpHeaderNames.TRANSFER_ENCODING, Consts.CHUNKED);
        }
        if (response.getHeader(HttpHeaderNames.HOST) == null) {
            response.setHeader(HttpHeaderNames.HOST, "smart-socket");
        }
        doNext(request, response);
    }
}
