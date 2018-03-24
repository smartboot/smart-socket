/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: CheckFilterGroup.java
 * Date: 2018-02-06
 * Author: sandao
 */

package org.smartboot.socket.http.http11;

import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.http11.request.HostCheckHandle;
import org.smartboot.socket.http.http11.request.MethodCheckHandle;
import org.smartboot.socket.http.http11.request.RouteHandle;
import org.smartboot.socket.http.http11.request.URICheckHandle;
import org.smartboot.socket.http.http11.response.DefaultHandle;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class Http11HandleGroup {

    private HttpHandle preHandle;
    private RouteHandle routeHandle;
    private HttpHandle lastHandle;

    public Http11HandleGroup(String baseDir) {
        routeHandle = new RouteHandle(baseDir);
        preHandle = new MethodCheckHandle();
        preHandle.next(new HostCheckHandle()).next(new URICheckHandle());

        preHandle.next(routeHandle);

        lastHandle = new DefaultHandle();
    }

    public HttpHandle getPreHandle() {
        return preHandle;
    }

    public HttpHandle getLastHandle() {
        return lastHandle;
    }

    public RouteHandle getRouteHandle() {
        return routeHandle;
    }
}
