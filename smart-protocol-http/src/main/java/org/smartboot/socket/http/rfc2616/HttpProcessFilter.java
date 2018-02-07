/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpProcessFilter.java
 * Date: 2018-02-07
 * Author: sandao
 */

package org.smartboot.socket.http.rfc2616;

import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.route.Route;
import org.smartboot.socket.http.route.StaticResourceRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public class HttpProcessFilter extends HttpFilter {
    private List<Route> routeList = new ArrayList<>();

    {
        routeList.add(new StaticResourceRoute());
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response) throws IOException {
        Route route = null;
        for (Route r : routeList) {
            if (request.getOriginalUri().matches(r.urlPattern())) {
                route = r;
                break;
            }
        }
        if (route == null) {
            response.setHttpStatus(HttpStatus.NOT_FOUND);
            return;
        }
        route.process(request, response);
    }
}
