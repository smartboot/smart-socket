/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: URICheckHandle.java
 * Date: 2018-02-08
 * Author: sandao
 */

package org.smartboot.socket.http.http11.request;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.http11.Http11Request;

import java.io.IOException;

/**
 * RFC2616 3.2.1
 * HTTP 协议不对 URI 的长度作事先的限制，服务器必须能够处理任何他们提供资源的 URI，并 且应该能够处理无限长度的 URIs，这种无效长度的 URL 可能会在客户端以基于 GET 方式的 请求时产生。如果服务器不能处理太长的 URI 的时候，服务器应该返回 414 状态码(此状态码 代表 Request-URI 太长)。
 * 注:服务器在依赖大于 255 字节的 URI 时应谨慎，因为一些旧的客户或代理实现可能不支持这 些长度。
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class URICheckHandle extends HttpHandle {
    public static final int MAX_LENGTH = 255 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(URICheckHandle.class);

    @Override
    public void doHandle(Http11Request request, HttpResponse response) throws IOException {

        if (StringUtils.length(request.getOriginalUri()) > MAX_LENGTH) {
            response.setHttpStatus(HttpStatus.URI_TOO_LONG);
            return;
        }
        parseOriginalUri(request);
        doNext(request, response);
    }

    /**
     * @param request
     */
    private void parseOriginalUri(Http11Request request) {
        /**
         *http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
         *1. 如果 Request-URI 是绝对地址(absoluteURI)，那么主机(host)是 Request-URI 的 一部分。任何出现在请求里 Host 头域的值应当被忽略。
         *2. 假如 Request-URI 不是绝对地址(absoluteURI)，并且请求包括一个 Host 头域，则主 机(host)由该 Host 头域的值决定.
         *3. 假如由规则1或规则2定义的主机(host)对服务器来说是一个无效的主机(host)， 则应当以一个 400(坏请求)错误消息返回。
         */
        String originalUri = request.getOriginalUri();
        int schemeIndex = originalUri.indexOf("://");
        int queryStringIndex = StringUtils.indexOf(originalUri, "?");
        if (queryStringIndex != StringUtils.INDEX_NOT_FOUND) {
            request.setQueryString(StringUtils.substring(originalUri, queryStringIndex + 1));
        }

        if (schemeIndex > 0) {//绝对路径
//            request.setScheme(originalUri.substring(0, schemeIndex));
            int uriIndex = originalUri.indexOf('/', schemeIndex + 3);
            if (uriIndex == StringUtils.INDEX_NOT_FOUND) {
                request.setRequestURI("/");
            } else {

                request.setRequestURI(queryStringIndex > 0 ?
                        StringUtils.substring(originalUri, uriIndex, queryStringIndex)
                        : StringUtils.substring(originalUri, uriIndex));
            }

        } else {
            request.setRequestURI(queryStringIndex > 0 ?
                    StringUtils.substring(originalUri, 0, queryStringIndex)
                    : originalUri);
        }
    }

}
