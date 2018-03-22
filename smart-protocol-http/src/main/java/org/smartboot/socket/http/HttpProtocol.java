/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpV2Protocol.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.http.enums.HttpPartEnum;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.http11.Http11ContentDecoder;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.HttpHeaderConstant;
import org.smartboot.socket.http.websocket.DataFraming;
import org.smartboot.socket.http.websocket.WebsocketDecoder;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
final class HttpProtocol implements Protocol<HttpRequest> {

    private static final Logger LOGGER = LogManager.getLogger(HttpProtocol.class);
    private static final byte[] SP = {Consts.SP};

    private WebsocketDecoder websocketDecoder = new WebsocketDecoder();

    private Http11ContentDecoder http11ContentDecoder = new Http11ContentDecoder();

    @Override
    public HttpRequest decode(ByteBuffer buffer, AioSession<HttpRequest> session, boolean eof) {

        HttpDecodeUnit decodeUnit = getHttpDecodeUnit(session);

        while (buffer.hasRemaining() && decodeUnit.getDecodePartEnum() != HttpPartEnum.END) {
            switch (decodeUnit.getDecodePartEnum()) {
                //解析请求行
                case REQUEST_LINE_METHOD:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeRequestLineMethod(decodeUnit);
                    }
                    break;
                //解析请求行:URL
                case REQUEST_LINE_URL:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeRequestLineURL(decodeUnit);
                    }
                    break;
                //解析请求行:HTTP_VERSION
                case REQUEST_LINE_VERSION:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeHttpVersion(decodeUnit);
                    }
                    break;
                //解析消息头
                case HEAD_LINE:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeHeadLine(decodeUnit);
                    }
                    //本行仅有\r\n表示消息体部分已结束
                    if (decodeUnit.getDecodePartEnum() != HttpPartEnum.HEAD_END_LINE) {
                        break;
                    }
                    //识别如何处理Body部分
                case HEAD_END_LINE: {
                    HttpHeader httpHeader = decodeUnit.getHeader();
                    //Websocket消息
                    if (StringUtils.equals(httpHeader.getHeader(HttpHeaderConstant.Names.CONNECTION), HttpHeaderConstant.Values.UPGRADE)
                            && StringUtils.equals(httpHeader.getHeader(HttpHeaderConstant.Names.UPGRADE), HttpHeaderConstant.Values.WEBSOCKET)) {
                        decodeUnit.setContentDecoder(websocketDecoder);
                        DataFraming dataFraming = new DataFraming(httpHeader);
                        decodeUnit.setEntity(dataFraming);
                    }
                    //Http1.1协议
                    else {
                        decodeUnit.setContentDecoder(http11ContentDecoder);
                        Http11Request request = new Http11Request(httpHeader);
                        decodeUnit.setEntity(request);
                    }
                    decodeUnit.setDecodePartEnum(HttpPartEnum.CONTENT);
                }
                case CONTENT: {
                    HttpRequest request = decodeUnit.getContentDecoder().decode(buffer, session, eof);
                    if (request != null) {
                        decodeUnit.setDecodePartEnum(HttpPartEnum.END);
                    }
                    break;
                }
                default: {
                    session.setAttachment(null);
                }
            }
        }
        if (decodeUnit.getDecodePartEnum() == HttpPartEnum.END) {
            session.setAttachment(null);
            return decodeUnit.getEntity();
        } else {
            return null;
        }
    }

    /**
     * 获得解码对象模型
     *
     * @param session
     * @return
     */
    private HttpDecodeUnit getHttpDecodeUnit(AioSession<HttpRequest> session) {
        HttpDecodeUnit decodeUnit = null;
        if (session.getAttachment() == null) {
            decodeUnit = new HttpDecodeUnit();
            decodeUnit.setHeader(new HttpHeader());
//            decodeUnit.entity = new HttpRequest(session);
            decodeUnit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_METHOD);
            decodeUnit.setHeadPartDecoder(new DelimiterFrameDecoder(SP, 10));
            session.setAttachment(decodeUnit);
        } else {
            decodeUnit = (HttpDecodeUnit) session.getAttachment();
        }
        if (decodeUnit.getDecodePartEnum() == HttpPartEnum.END) {
            HttpHeader httpHeader = decodeUnit.getHeader();
            httpHeader.setHttpVersion(null);
            httpHeader.setMethod(null);
            httpHeader.setOriginalUri(null);
            httpHeader.headerMap.clear();
            decodeUnit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_METHOD);
            decodeUnit.getHeadPartDecoder().reset(SP);
        }
        return decodeUnit;
    }

    @Override
    public ByteBuffer encode(HttpRequest httpRequest, AioSession<HttpRequest> session) {
        return null;
    }


    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeRequestLineMethod(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.getHeadPartDecoder().getBuffer();
        unit.getHeader().setMethod(MethodEnum.getByMethod(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length));

        //识别一下一个解码阶段
        unit.getHeadPartDecoder().reset();
        unit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_URL);
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeRequestLineURL(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.getHeadPartDecoder().getBuffer();
        String uri = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length);

        unit.getHeader().setOriginalUri(uri);

        //识别一下一个解码阶段
        unit.getHeadPartDecoder().reset(Consts.CRLF);
        unit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_VERSION);
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeHttpVersion(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.getHeadPartDecoder().getBuffer();
        String httpVersion = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - Consts.CRLF.length);

        unit.getHeader().setHttpVersion(httpVersion);

        //识别一下一个解码阶段
        unit.getHeadPartDecoder().reset();
        unit.setDecodePartEnum(HttpPartEnum.HEAD_LINE);
    }

    /**
     * 解析消息头
     *
     * @param unit
     */
    private void decodeHeadLine(HttpDecodeUnit unit) {
        ByteBuffer headLineBuffer = unit.getHeadPartDecoder().getBuffer();

        //消息头已结束
        if (headLineBuffer.remaining() == Consts.CRLF.length) {
            unit.setDecodePartEnum(HttpPartEnum.HEAD_END_LINE);
            unit.getHeadPartDecoder().reset();
            return;
        }
        String headLine = new String(headLineBuffer.array(), 0, headLineBuffer.remaining());

        unit.getHeader().setHeader(StringUtils.substringBefore(headLine, ":"), StringUtils.substringAfter(headLine, ":").trim());

        //识别一下一个解码阶段
        unit.getHeadPartDecoder().reset();
        unit.setDecodePartEnum(HttpPartEnum.HEAD_LINE);
    }
}
