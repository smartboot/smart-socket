/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StaticResourceRoute.java
 * Date: 2018-02-07
 * Author: sandao
 */

package org.smartboot.socket.http.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.enums.HttpStatus;
import org.smartboot.socket.http.http11.Http11Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 静态资源加载Handle
 *
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public class StaticResourceHandle extends HttpHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceHandle.class);
    private static final int READ_BUFFER = 1024;
    private String baseDir;

    public StaticResourceHandle(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void doHandle(Http11Request request, HttpResponse response) throws IOException {
        File file = new File(baseDir + request.getRequestURI());
        if (!file.isFile()) {
            LOGGER.warn("file:{} not found!", request.getRequestURI());
            response.setHttpStatus(HttpStatus.NOT_FOUND);
            return;
        }
        FileInputStream fis = new FileInputStream(file);
        FileChannel fileChannel = fis.getChannel();
        long fileSize = fileChannel.size();
        long readPos = 0;
        while (readPos < fileSize) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, readPos, fileSize - readPos > READ_BUFFER ? READ_BUFFER : fileSize - readPos);
            readPos += mappedByteBuffer.remaining();
            response.write(mappedByteBuffer);
        }
        fis.close();
    }
}
