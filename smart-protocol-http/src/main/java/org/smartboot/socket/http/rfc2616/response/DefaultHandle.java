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
import org.smartboot.socket.http.utils.HttpHeader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/8
 */
public class DefaultHandle extends HttpHandle {
    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println(sdf.format(new Date()));
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException {
        if (response.getHttpStatus() == null) {
            response.setHttpStatus(HttpStatus.OK);
        }
        if (response.getHeader(HttpHeader.Names.CONTENT_LENGTH) == null && response.getHeader(HttpHeader.Names.TRANSFER_ENCODING) == null
                && response.getHttpStatus() == HttpStatus.OK) {
            response.setHeader(HttpHeader.Names.TRANSFER_ENCODING, HttpHeader.Values.CHUNKED);
        }
        if (response.getHeader(HttpHeader.Names.SERVER) == null) {
            response.setHeader(HttpHeader.Names.SERVER, "smart-sockets");
        }
        if (response.getHeader(HttpHeader.Names.HOST) == null) {
            response.setHeader(HttpHeader.Names.HOST, "localhost");
        }

        /**
         * RFC2616 3.3.1
         * 只能用 RFC 1123 里定义的日期格式来填充头域 (header field)的值里用到 HTTP-date 的地方
         */
        SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        response.setHeader(HttpHeader.Names.DATE, sdf.format(new Date()));

        doNext(request, response);
    }
}
