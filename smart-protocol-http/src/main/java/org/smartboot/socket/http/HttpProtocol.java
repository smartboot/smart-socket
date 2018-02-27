/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpV2Protocol.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.extension.decoder.StreamFrameDecoder;
import org.smartboot.socket.http.enums.BodyTypeEnum;
import org.smartboot.socket.http.enums.HttpPartEnum;
import org.smartboot.socket.http.enums.MethodEnum;
import org.smartboot.socket.http.http11.Http11ContentDecoder;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.EmptyInputStream;
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
                    if (decodeUnit.headPartDecoder.decode(buffer)) {
                        decodeRequestLineMethod(decodeUnit);
                    }
                    break;
                //解析请求行:URL
                case REQUEST_LINE_URL:
                    if (decodeUnit.headPartDecoder.decode(buffer)) {
                        decodeRequestLineURL(decodeUnit);
                    }
                    break;
                //解析请求行:HTTP_VERSION
                case REQUEST_LINE_VERSION:
                    if (decodeUnit.headPartDecoder.decode(buffer)) {
                        decodeHttpVersion(decodeUnit);
                    }
                    break;
                //解析消息头
                case HEAD_LINE:
                    if (decodeUnit.headPartDecoder.decode(buffer)) {
                        decodeHeadLine(decodeUnit);
                    }
                    //本行仅有\r\n表示消息体部分已结束
                    if (decodeUnit.getDecodePartEnum() != HttpPartEnum.HEAD_END_LINE) {
                        break;
                    }
                    //识别如何处理Body部分
                case HEAD_END_LINE: {
                    HttpHeader httpHeader = decodeUnit.header;
                    //Websocket消息
                    if (StringUtils.equals(httpHeader.getHeader(HttpHeaderConstant.Names.CONNECTION), HttpHeaderConstant.Values.UPGRADE)
                            && StringUtils.equals(httpHeader.getHeader(HttpHeaderConstant.Names.UPGRADE), HttpHeaderConstant.Values.WEBSOCKET)) {
                        decodeUnit.contentDecoder = websocketDecoder;
                        DataFraming dataFraming = new DataFraming(httpHeader);
                        decodeUnit.entity = dataFraming;
                    }
                    //Http1.1协议
                    else {
                        decodeUnit.contentDecoder = http11ContentDecoder;
                        Http11Request request = new Http11Request(httpHeader);
                        decodeUnit.entity = request;
                        if (MethodEnum.POST != request.getHeader().getMethod()) {
                            decodeUnit.setDecodePartEnum(HttpPartEnum.END);
                            decodeUnit.setPartFinished(true);
                            break;
                        } else {
                            //识别Body解码器
                            String contentType = httpHeader.getHeader(HttpHeaderConstant.Names.CONTENT_TYPE);
                            int contentLength = NumberUtils.toInt(httpHeader.getHeader(HttpHeaderConstant.Names.CONTENT_LENGTH), -1);
                            if (StringUtils.startsWith(contentType, HttpHeaderConstant.Values.MULTIPART_FORM_DATA)) {
                                decodeUnit.bodyTypeEnum = BodyTypeEnum.STREAM;
                                decodeUnit.setPartFinished(true);
                                decodeUnit.streamBodyDecoder = new StreamFrameDecoder(contentLength);
                                request.setInputStream(decodeUnit.streamBodyDecoder.getInputStream());
                            } else {
                                decodeUnit.bodyTypeEnum = BodyTypeEnum.FORM;
                                decodeUnit.formBodyDecoder = new FixedLengthFrameDecoder(contentLength);
                                request.setInputStream(new EmptyInputStream());
                            }
                        }
                    }
                    decodeUnit.setDecodePartEnum(HttpPartEnum.CONTENT);
                }
                case CONTENT: {
                    decodeUnit.contentDecoder.decode(decodeUnit, buffer);
                    break;
                }
                default: {
                    session.setAttachment(null);
                }
            }
        }
        if (decodeUnit.getDecodePartEnum() == HttpPartEnum.END) {
            session.setAttachment(null);
        }
        if (decodeUnit.isPartFinished()) {
            decodeUnit.setPartFinished(false);//重置状态
            return decodeUnit.entity;
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
            decodeUnit.header = new HttpHeader();
//            decodeUnit.entity = new HttpRequest(session);
            decodeUnit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_METHOD);
            decodeUnit.headPartDecoder = new DelimiterFrameDecoder(SP, 10);
            session.setAttachment(decodeUnit);
        } else {
            decodeUnit = (HttpDecodeUnit) session.getAttachment();
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
        ByteBuffer requestLineBuffer = unit.headPartDecoder.getBuffer();
        unit.header.setMethod(MethodEnum.getByMethod(new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length)));

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_URL);
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeRequestLineURL(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.headPartDecoder.getBuffer();
        String uri = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length);

        unit.header.setOriginalUri(uri);

        //识别一下一个解码阶段
        unit.headPartDecoder.reset(Consts.CRLF);
        unit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_VERSION);
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeHttpVersion(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.headPartDecoder.getBuffer();
        String httpVersion = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - Consts.CRLF.length);

        unit.header.setHttpVersion(httpVersion);

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.setDecodePartEnum(HttpPartEnum.HEAD_LINE);
    }

    /**
     * 解析消息头
     *
     * @param unit
     */
    private void decodeHeadLine(HttpDecodeUnit unit) {
        ByteBuffer headLineBuffer = unit.headPartDecoder.getBuffer();

        //消息头已结束
        if (headLineBuffer.remaining() == Consts.CRLF.length) {
            unit.setDecodePartEnum(HttpPartEnum.HEAD_END_LINE);
            unit.headPartDecoder.reset();
            return;
        }
        String headLine = new String(headLineBuffer.array(), 0, headLineBuffer.remaining());

        unit.header.setHeader(StringUtils.substringBefore(headLine, ":"), StringUtils.substringAfter(headLine, ":").trim());

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.setDecodePartEnum(HttpPartEnum.HEAD_LINE);
    }
}
