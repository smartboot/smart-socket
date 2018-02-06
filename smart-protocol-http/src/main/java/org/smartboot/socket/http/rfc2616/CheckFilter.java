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

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class CheckFilter {

    private CheckFilter nextFilter;

    public abstract void doFilter(HttpRequest request, HttpResponse response);

    public final void doNext(HttpRequest request, HttpResponse response) {
        if (nextFilter != null) {
            nextFilter.doFilter(request, response);
        }
    }

    public final CheckFilter next(CheckFilter nextFilter) {
        this.nextFilter = nextFilter;
        return nextFilter;
    }
}
