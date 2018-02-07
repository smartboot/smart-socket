/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StaticResourceRoute.java
 * Date: 2018-02-07
 * Author: sandao
 */

package org.smartboot.socket.http.handle;

import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.rfc2616.HttpHandle;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public class StaticResourceHandle extends HttpHandle {
    private String baseDir="/Users/zhengjunwei/Downloads";
    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException {
        response.getOutputStream().write(request.toString().getBytes());
    }
}
