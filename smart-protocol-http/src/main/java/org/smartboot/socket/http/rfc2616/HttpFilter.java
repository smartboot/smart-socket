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
public abstract class HttpFilter {

    private HttpFilter nextFilter;

    public abstract void doFilter(HttpRequest request, HttpResponse response) throws IOException;

    final void doNext(HttpRequest request, HttpResponse response) throws IOException {
        if (nextFilter != null) {
            nextFilter.doFilter(request, response);
        }
    }

    /**
     * 添加CheckFilter至末尾
     *
     * @param nextFilter
     * @return
     */
    public final HttpFilter next(HttpFilter nextFilter) {
        HttpFilter lasterFilter = this;
        while (lasterFilter.nextFilter != null) {
            lasterFilter = lasterFilter.nextFilter;
        }
        lasterFilter.nextFilter = nextFilter;
        return this;
    }
}
