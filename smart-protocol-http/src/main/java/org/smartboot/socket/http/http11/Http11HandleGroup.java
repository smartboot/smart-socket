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
import org.smartboot.socket.http.http11.request.URICheckHandle;
import org.smartboot.socket.http.http11.response.DefaultHandle;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public class Http11HandleGroup {

    private static Http11HandleGroup group;
    private HttpHandle httpHandle;

    private HttpHandle lastHandle;

    private Http11HandleGroup() {
        httpHandle = new MethodCheckHandle();
        httpHandle.next(new HostCheckHandle()).next(new URICheckHandle());

//        httpFilter.next(new HttpProcessFilter());
        lastHandle = new DefaultHandle();
    }

    public static final Http11HandleGroup group() {
        if (group != null) {
            return group;
        }
        synchronized (Http11HandleGroup.class) {
            if (group != null) {
                return group;
            }
            group = new Http11HandleGroup();
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
