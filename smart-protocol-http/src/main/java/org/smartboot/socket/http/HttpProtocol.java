/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpV2Protocol.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.smartboot.socket.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
final class HttpProtocol implements Protocol<HttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProtocol.class);
    private static final int READ_BUFFER = 128;
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
                    } else {
                        break;
                    }
                    //解析请求行:URL
                case REQUEST_LINE_URL:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeRequestLineURL(decodeUnit);
                    } else {
                        break;
                    }
                    //解析请求行:HTTP_VERSION
                case REQUEST_LINE_VERSION:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        decodeHttpVersion(decodeUnit);
                    }
                    break;
                //解析消息头Key部分
                case HEAD_LINE_KEY:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        ByteBuffer headLineBuffer = decodeUnit.getHeadPartDecoder().getBuffer();
                        BufferUtils.trim(headLineBuffer);
                        decodeUnit.setHeaderKey(new String(headLineBuffer.array(), headLineBuffer.position(), headLineBuffer.remaining() - Consts.COLON_ARRAY.length));

                        //解析消息头Value部分
                        decodeUnit.getHeadPartDecoder().reset(Consts.CRLF);
                        decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_LINE_VALUE);
                    } else {
                        break;
                    }
                    //解析消息头Key部分
                case HEAD_LINE_VALUE:
                    if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                        ByteBuffer headLineBuffer = decodeUnit.getHeadPartDecoder().getBuffer();
                        BufferUtils.trim(headLineBuffer);
                        decodeUnit.getHeader().setHeader(decodeUnit.getHeaderKey(), new String(headLineBuffer.array(), headLineBuffer.position(), headLineBuffer.remaining()));
                        //检验Header解码是否结束
                        decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_END_CHECK);
                    } else {
                        break;
                    }
                case HEAD_END_CHECK:
                    if (buffer.remaining() < 2) {
                        return null;
                    }
                    //非/r/n视为普通header行
                    if (buffer.get(buffer.position()) != Consts.CR || buffer.get(buffer.position() + 1) != Consts.LF) {
                        //识别一下一个解码阶段
                        decodeUnit.getHeadPartDecoder().reset(Consts.COLON_ARRAY);
                        decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_LINE_KEY);
                        break;
                    }
                    buffer.position(buffer.position() + Consts.CRLF.length);
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
            decodeUnit.setHeadPartDecoder(new DelimiterFrameDecoder(Consts.SP_ARRAY, READ_BUFFER));
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
            decodeUnit.getHeadPartDecoder().reset(Consts.SP_ARRAY);
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
        unit.getHeader().setMethod(MethodEnum.getByMethod(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - Consts.SP_ARRAY.length));

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
        String uri = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - Consts.SP_ARRAY.length);

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
        byte[] versionBytes = new byte[requestLineBuffer.remaining() - Consts.CRLF.length];
        System.arraycopy(requestLineBuffer.array(), 0, versionBytes, 0, versionBytes.length);
//        String httpVersion = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - Consts.CRLF.length);

        unit.getHeader().setVersionBytes(versionBytes);

        //识别一下一个解码阶段
        unit.getHeadPartDecoder().reset(Consts.COLON_ARRAY);
        unit.setDecodePartEnum(HttpPartEnum.HEAD_END_CHECK);
    }

}
