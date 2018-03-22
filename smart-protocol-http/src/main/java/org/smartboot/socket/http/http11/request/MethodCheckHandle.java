/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: MethodCheckHandle.java
 * Date: 2018-02-08
 * Author: sandao
 */

package org.smartboot.socket.http.http11.request;

import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.http11.Http11Request;

import java.io.IOException;

/**
 * RFC2616 5.1.1
 * 方法标记指明了在被 Request-URI 指定的资源上执行的方法。这种方法是大小写敏感的。
 * 资源所允许的方法由 Allow 头域指定(14.7 节)。
 * 响应的返回码总是通知客户某个方法对当前资源是否是被允许的，因为被允许的方法能被动态的改变。
 * 如果服务器能理解某方法但此方法对请求资源不被允许的，那么源服务器应该返回 405 状态码(方法不允许);
 * 如果源服务器不能识别或没有实现某个方法，那么服务器应返回 501 状态码(没有实现)。
 * 方法 GET 和 HEAD 必须被所有一般的服务器支持。
 * 所有其它的方法是可选的;
 * 然而，如果上面的方法都被实现， 这些方法遵循的语意必须和第 9 章指定的相同。
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class MethodCheckHandle extends HttpHandle {
    @Override
    public void doHandle(Http11Request request, HttpResponse response) throws IOException {
        MethodEnum methodEnum = request.getMethod();//大小写敏感
        if (methodEnum == null) {
            response.setHttpStatus(HttpStatus.NOT_IMPLEMENTED);
            return;
        }

        //暂时只支持GET/POST
        if (methodEnum == MethodEnum.GET || methodEnum == MethodEnum.POST) {
            doNext(request, response);
        } else {
            response.setHttpStatus(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
