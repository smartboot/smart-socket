/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: URICheckFilter.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;

import java.io.IOException;

/**
 * RFC2616 3.2.1
 * HTTP 协议不对 URI 的长度作事先的限制，服务器必须能够处理任何他们提供资源的 URI，并 且应该能够处理无限长度的 URIs，这种无效长度的 URL 可能会在客户端以基于 GET 方式的 请求时产生。如果服务器不能处理太长的 URI 的时候，服务器应该返回 414 状态码(此状态码 代表 Request-URI 太长)。
 * 注:服务器在依赖大于 255 字节的 URI 时应谨慎，因为一些旧的客户或代理实现可能不支持这 些长度。
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class URICheckFilter extends HttpFilter {
    public static final int MAX_LENGTH = 255 * 1024;
    private static final Logger LOGGER = LogManager.getLogger(URICheckFilter.class);

    @Override
    public void doFilter(HttpRequest request, HttpResponse response) throws IOException {

        if (StringUtils.length(request.getOriginalUri()) > MAX_LENGTH) {
            response.setHttpStatus(HttpStatus.URI_TOO_LONG);
            return;
        }

        doNext(request, response);
    }

    /**
     * @param request
     * @deprecated
     */
//    private void decodeOriginalUri(HttpRequest request) {
//        /**
//         *http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
//         *1. 如果 Request-URI 是绝对地址(absoluteURI)，那么主机(host)是 Request-URI 的 一部分。任何出现在请求里 Host 头域的值应当被忽略。
//         *2. 假如 Request-URI 不是绝对地址(absoluteURI)，并且请求包括一个 Host 头域，则主 机(host)由该 Host 头域的值决定.
//         *3. 假如由规则1或规则2定义的主机(host)对服务器来说是一个无效的主机(host)， 则应当以一个 400(坏请求)错误消息返回。
//         */
//        String originalUri = request.getOriginalUri();
//        boolean absoulute = !StringUtils.startsWith(originalUri, "/");//是否绝对路径
//        String queryString = StringUtils.substringAfter(originalUri, "?");
//        request.setQueryString(queryString);
//        //去除query部分
//        if (StringUtils.isNotBlank(queryString)) {
//            originalUri = StringUtils.substringBefore(originalUri, "?");
//        }
//        String headHost = request.getHeader(HttpHeaderNames.HOST);
//
//        if (absoulute) {
//            if (!StringUtils.isBlank(headHost)) {
//                LOGGER.debug("absoulute originalUri:{} ,ignore HEAD HOST:{}", originalUri, headHost);
//            }
//            request.setScheme(StringUtils.substringBefore(originalUri, ":"));
//            originalUri = StringUtils.substringAfter(originalUri, "://");//去除scheme部分,host [ ":" port ] [ abs_path]s
//            if (originalUri.indexOf('/') == -1) {
//                originalUri = originalUri + "/";
//            }
//            if (originalUri.indexOf(":") > 0) {//包含端口号
//                request.setHost(StringUtils.substringBefore(originalUri, ":"));
//                request.setPort(NumberUtils.toInt(StringUtils.substringBetween(originalUri, ":", "/"), -1));
//            } else {
//                request.setHost(StringUtils.substringBefore(originalUri, "/"));
//                request.setPort(80);
//            }
//            request.setRequestURI("/" + StringUtils.substringAfter(originalUri, "/"));
//        } else {
//            if (headHost.indexOf(":") > 0) {
//                request.setHost(StringUtils.substringBefore(headHost, ":"));
//                request.setPort(NumberUtils.toInt(StringUtils.substringAfter(headHost, ":"), -1));
//            } else {
//                request.setHost(headHost);
//                request.setPort(80);
//            }
//            request.setRequestURI(originalUri);
//        }
//    }
}
