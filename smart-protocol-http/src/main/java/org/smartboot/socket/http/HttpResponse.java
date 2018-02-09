/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpResponse.java
 * Date: 2018-02-03
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.http.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public interface HttpResponse {


    public OutputStream getOutputStream();

    public HttpStatus getHttpStatus();

    public void setHttpStatus(HttpStatus httpStatus);

    public void setHeader(String name, String value);

    public String getHeader(String name);

    public void write(ByteBuffer buffer) throws IOException;
}
