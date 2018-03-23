/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpDecodeUnit.java
 * Date: 2018-02-16
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.http.enums.HttpPartEnum;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/16
 */
public class HttpDecodeUnit {
    private HttpHeader header;
    private HttpRequest entity;
    private String headerKey;
    private Protocol<HttpRequest> contentDecoder;
    /**
     * 结束标解码器
     */
    private DelimiterFrameDecoder headPartDecoder;
    /**
     * 当前解码阶段
     */
    private HttpPartEnum decodePartEnum;

    private FixedLengthFrameDecoder formDecoder;

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

    public Protocol<HttpRequest> getContentDecoder() {
        return contentDecoder;
    }

    public void setContentDecoder(Protocol<HttpRequest> contentDecoder) {
        this.contentDecoder = contentDecoder;
    }

    public HttpPartEnum getDecodePartEnum() {
        return decodePartEnum;
    }

    public void setDecodePartEnum(HttpPartEnum decodePartEnum) {
        this.decodePartEnum = decodePartEnum;
    }

    public DelimiterFrameDecoder getHeadPartDecoder() {
        return headPartDecoder;
    }

    public void setHeadPartDecoder(DelimiterFrameDecoder headPartDecoder) {
        this.headPartDecoder = headPartDecoder;
    }

    public FixedLengthFrameDecoder getFormDecoder() {
        return formDecoder;
    }

    public void setFormDecoder(FixedLengthFrameDecoder formDecoder) {
        this.formDecoder = formDecoder;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public void setHeaderKey(String headerKey) {
        this.headerKey = headerKey;
    }


}
