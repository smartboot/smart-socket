/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StaticResourceRoute.java
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
public class StaticResourceRoute implements Route {

    private String baseDir;

    @Override
    public String urlPattern() {
        return ".*";
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws IOException {
        response.getOutputStream().write(request.toString().getBytes());
//        OutputStream os = response.getOutputStream();
//        FileInputStream inputStream = new FileInputStream(baseDir + request.getOriginalUri());
//        byte[] b = new byte[1024];
//        int readSize = 0;
//        while ((readSize = inputStream.read(b)) > -1) {
//            os.write(b, 0, readSize);
//        }
//        inputStream.close();
    }
}
