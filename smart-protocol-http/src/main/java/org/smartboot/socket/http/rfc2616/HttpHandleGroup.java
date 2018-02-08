/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: CheckFilterGroup.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

import org.smartboot.socket.http.rfc2616.request.HostCheckHandle;
import org.smartboot.socket.http.rfc2616.request.MethodCheckHandle;
import org.smartboot.socket.http.rfc2616.request.URICheckHandle;
import org.smartboot.socket.http.rfc2616.response.DefaultHandle;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class HttpHandleGroup {

    private static HttpHandleGroup group;
    private HttpHandle httpHandle;

    private HttpHandle lastHandle;

    private HttpHandleGroup() {
        httpHandle = new MethodCheckHandle();
        httpHandle.next(new HostCheckHandle()).next(new URICheckHandle());

//        httpFilter.next(new HttpProcessFilter());
        lastHandle = new DefaultHandle();
    }

    public static final HttpHandleGroup group() {
        if (group != null) {
            return group;
        }
        synchronized (HttpHandleGroup.class) {
            if (group != null) {
                return group;
            }
            group = new HttpHandleGroup();
        }
        return group;
    }

    public HttpHandle getHttpHandle() {
        return httpHandle;
    }

    public HttpHandle getLastHandle() {
        return lastHandle;
    }
}
