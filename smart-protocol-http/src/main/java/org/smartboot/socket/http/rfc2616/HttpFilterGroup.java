/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: CheckFilterGroup.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class HttpFilterGroup {

    private static HttpFilterGroup group;
    private HttpFilter httpFilter;

    private HttpFilterGroup() {
        httpFilter = new MethodCheckFilter();
        httpFilter.next(new HostCheckFilter()).next(new URICheckFilter());
    }

    public static final HttpFilterGroup group() {
        if (group != null) {
            return group;
        }
        synchronized (HttpFilterGroup.class) {
            if (group != null) {
                return group;
            }
            group = new HttpFilterGroup();
        }
        return group;
    }

    public HttpFilter getCheckFilter() {
        return httpFilter;
    }
}
