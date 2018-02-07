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
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.extension.decoder.StreamFrameDecoder;
import org.smartboot.socket.http.enums.BodyTypeEnum;
import org.smartboot.socket.http.enums.HttpPartEnum;
import org.smartboot.socket.http.utils.Consts;
import org.smartboot.socket.http.utils.EmptyInputStream;
import org.smartboot.socket.http.utils.HttpHeaderNames;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
final class HttpProtocol implements Protocol<HttpRequest> {
    public static final byte[] CRLF = {Consts.CR, Consts.LF};
    private static final Logger LOGGER = LogManager.getLogger(HttpProtocol.class);
    private static final byte[] SP = {Consts.SP};

    @Override
    public HttpRequest decode(ByteBuffer buffer, AioSession<HttpRequest> session, boolean eof) {

        HttpDecodeUnit decodeUnit = getHttpDecodeUnit(session);
        HttpRequest entity = decodeUnit.entity;

        boolean returnEntity = false;//是否返回HttpEntity
        boolean continueDecode = true;//是否继续读取数据
        while (buffer.hasRemaining() && continueDecode) {
            switch (decodeUnit.partEnum) {
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
                    if (decodeUnit.partEnum != HttpPartEnum.HEAD_END_LINE) {
                        break;
                    }
                    //识别如何处理Body部分
                case HEAD_END_LINE: {
                    decodeHeadEnd(decodeUnit);
                    if (decodeUnit.partEnum == HttpPartEnum.END) {
                        returnEntity = true;
                        continueDecode = false;
                    }
                    //文件流消息
                    else if (decodeUnit.bodyTypeEnum == BodyTypeEnum.STREAM) {
                        returnEntity = true;
                    }
                    break;
                }
                case BODY: {
                    decodeBody(decodeUnit, buffer);
                    break;
                }
                default: {
                    session.setAttachment(null);
                }
            }
        }
        if (decodeUnit.partEnum == HttpPartEnum.END) {
            session.setAttachment(null);
        }
        return returnEntity ? entity : null;
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
            decodeUnit.entity = new HttpRequest(session);
            decodeUnit.partEnum = HttpPartEnum.REQUEST_LINE_METHOD;
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
        HttpRequest request = unit.entity;
        request.setMethod(new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length));

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.partEnum = HttpPartEnum.REQUEST_LINE_URL;
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeRequestLineURL(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.headPartDecoder.getBuffer();
        String uri = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - SP.length);

        unit.entity.setOriginalUri(uri);

        //识别一下一个解码阶段
        unit.headPartDecoder.reset(CRLF);
        unit.partEnum = HttpPartEnum.REQUEST_LINE_VERSION;
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeHttpVersion(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.headPartDecoder.getBuffer();
        String httpVersion = new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining() - CRLF.length);

        HttpRequest request = unit.entity;
        request.setProtocol(httpVersion);

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.partEnum = HttpPartEnum.HEAD_LINE;
    }

    /**
     * 解析消息头
     *
     * @param unit
     */
    private void decodeHeadLine(HttpDecodeUnit unit) {
        ByteBuffer headLineBuffer = unit.headPartDecoder.getBuffer();

        //消息头已结束
        if (headLineBuffer.remaining() == CRLF.length) {
            unit.partEnum = HttpPartEnum.HEAD_END_LINE;
            unit.headPartDecoder.reset();
            return;
        }
        String headLine = new String(headLineBuffer.array(), 0, headLineBuffer.remaining());

        unit.entity.setHeader(StringUtils.substringBefore(headLine, ":"), StringUtils.substringAfter(headLine, ":").trim());

        //识别一下一个解码阶段
        unit.headPartDecoder.reset();
        unit.partEnum = HttpPartEnum.HEAD_LINE;
    }

    private void decodeHeadEnd(HttpDecodeUnit unit) {

        if (!StringUtils.equals("POST", unit.entity.getMethod())) {
            unit.partEnum = HttpPartEnum.END;
            return;
        }
        unit.partEnum = HttpPartEnum.BODY;
        //识别Body解码器
        String contentType = unit.entity.getHeader(HttpHeaderNames.CONTENT_TYPE);
        int contentLength = unit.entity.getContentLength();
        if (StringUtils.startsWith(contentType, "multipart/form-data")) {
            unit.bodyTypeEnum = BodyTypeEnum.STREAM;
            unit.streamBodyDecoder = new StreamFrameDecoder(contentLength);
            unit.entity.setInputStream(unit.streamBodyDecoder.getInputStream());
        } else {
            unit.bodyTypeEnum = BodyTypeEnum.FORM;
            unit.formBodyDecoder = new FixedLengthFrameDecoder(contentLength);
            unit.entity.setInputStream(new EmptyInputStream());
        }
    }

    private void decodeBody(HttpDecodeUnit unit, ByteBuffer buffer) {
        switch (unit.bodyTypeEnum) {
            case FORM:
                if (unit.formBodyDecoder.decode(buffer)) {
                    decodeBodyForm(unit);
                    unit.partEnum = HttpPartEnum.END;
                }
                break;
            case STREAM:
                if (unit.streamBodyDecoder.decode(buffer)) {
                    unit.partEnum = HttpPartEnum.END;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }


    private void decodeBodyForm(HttpDecodeUnit unit) {
        ByteBuffer buffer = unit.formBodyDecoder.getBuffer();
        String[] paramArray = StringUtils.split(new String(buffer.array(), buffer.position(), buffer.remaining()), "&");
        for (int i = 0; i < paramArray.length; i++) {
            unit.entity.setParam(StringUtils.substringBefore(paramArray[i], "=").trim(), StringUtils.substringAfter(paramArray[i], "=").trim());
        }
    }

    class HttpDecodeUnit {


        HttpRequest entity;
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
    }
}
