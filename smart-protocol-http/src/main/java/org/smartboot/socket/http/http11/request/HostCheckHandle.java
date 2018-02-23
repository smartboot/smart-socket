/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HostCheckHandle.java
 * Date: 2018-02-08
 * Author: sandao
 */

package org.smartboot.socket.http.http11.request;

import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.http.utils.HttpHeaderConstant;

import java.io.IOException;

/**
 * 1、客户端和服务器都必须支持 Host 请求头域。
 * 2、发送 HTTP/1.1 请求的客户端必须发送 Host 头域。
 * 3、如果 HTTP/1.1 请求不包括 Host 请求头域，服务器必须报告错误 400(Bad Request)。 --服务器必须接受绝对 URIs(absolute URIs)。
 *
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public class HostCheckHandle extends HttpHandle {
    @Override
    public void doHandle(Http11Request request, HttpResponse response) throws IOException {
        if (request.getHeader(HttpHeaderConstant.Names.HOST) == null) {
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            return;
        }
        doNext(request, response);
    }
}
