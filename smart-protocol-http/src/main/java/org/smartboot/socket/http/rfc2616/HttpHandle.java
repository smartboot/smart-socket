/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: CheckFilter.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class HttpHandle {

    private HttpHandle nextHandle;

    public abstract void doHandle(HttpRequest request, HttpResponse response) throws IOException;

    final void doNext(HttpRequest request, HttpResponse response) throws IOException {
        if (nextHandle != null) {
            nextHandle.doHandle(request, response);
        }
    }

    /**
     * 添加CheckFilter至末尾
     *
     * @param nextFilter
     * @return
     */
    public final HttpHandle next(HttpHandle nextFilter) {
        HttpHandle httpHandle = this;
        while (httpHandle.nextHandle != null) {
            httpHandle = httpHandle.nextHandle;
        }
        httpHandle.nextHandle = nextFilter;
        return this;
    }
}
