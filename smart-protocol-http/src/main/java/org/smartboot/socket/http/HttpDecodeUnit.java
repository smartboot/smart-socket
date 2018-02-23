/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpDecodeUnit.java
 * Date: 2018-02-16
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.extension.decoder.StreamFrameDecoder;
import org.smartboot.socket.http.enums.BodyTypeEnum;
import org.smartboot.socket.http.enums.HttpPartEnum;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public class HttpDecodeUnit {
    HttpHeader header;
    HttpRequest entity;

    boolean returnEntity;

    HttpContentDecoder contentDecoder;

    /**
     * 当前解码阶段
     */
    HttpPartEnum partEnum;

    BodyTypeEnum bodyTypeEnum;
    /**
     * 结束标解码器
     */
    DelimiterFrameDecoder headPartDecoder;

    FixedLengthFrameDecoder formBodyDecoder;

    StreamFrameDecoder streamBodyDecoder;

    public HttpHeader getHeader() {
        return header;
    }

    public void setHeader(HttpHeader header) {
        this.header = header;
    }

    public HttpRequest getEntity() {
        return entity;
    }

    public void setEntity(HttpRequest entity) {
        this.entity = entity;
    }

    public boolean isReturnEntity() {
        return returnEntity;
    }

    public void setReturnEntity(boolean returnEntity) {
        this.returnEntity = returnEntity;
    }

    public HttpContentDecoder getContentDecoder() {
        return contentDecoder;
    }

    public void setContentDecoder(HttpContentDecoder contentDecoder) {
        this.contentDecoder = contentDecoder;
    }

    public HttpPartEnum getPartEnum() {
        return partEnum;
    }

    public void setPartEnum(HttpPartEnum partEnum) {
        this.partEnum = partEnum;
    }

    public BodyTypeEnum getBodyTypeEnum() {
        return bodyTypeEnum;
    }

    public void setBodyTypeEnum(BodyTypeEnum bodyTypeEnum) {
        this.bodyTypeEnum = bodyTypeEnum;
    }

    public DelimiterFrameDecoder getHeadPartDecoder() {
        return headPartDecoder;
    }

    public void setHeadPartDecoder(DelimiterFrameDecoder headPartDecoder) {
        this.headPartDecoder = headPartDecoder;
    }

    public FixedLengthFrameDecoder getFormBodyDecoder() {
        return formBodyDecoder;
    }

    public void setFormBodyDecoder(FixedLengthFrameDecoder formBodyDecoder) {
        this.formBodyDecoder = formBodyDecoder;
    }

    public StreamFrameDecoder getStreamBodyDecoder() {
        return streamBodyDecoder;
    }

    public void setStreamBodyDecoder(StreamFrameDecoder streamBodyDecoder) {
        this.streamBodyDecoder = streamBodyDecoder;
    }
}
