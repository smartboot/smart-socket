/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Route.java
 * Date: 2018-02-07
 * Author: sandao
 */

package org.smartboot.socket.http.route;

import org.smartboot.socket.http.HttpRequest;
import org.smartboot.socket.http.HttpResponse;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public interface Route {

    String urlPattern();

    void process(HttpRequest request, HttpResponse response) throws IOException;
}
