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

    public static void main(String[] args) {
        System.out.println((char) ('a' | 0x20));
        System.out.println((char) ('Z' | 0x20));
        byte b = '%';
        System.out.println(b >> 5 & (1 << (b & 0x1f)));
    }

    @Override
    public HttpRequest decode(ByteBuffer buffer, AioSession<HttpRequest> session, boolean eof) {

        HttpDecodeUnit decodeUnit = getHttpDecodeUnit(session, buffer);
        HttpPartEnum startPart = decodeUnit.getDecodePartEnum();
        buffer.mark();
        try {
            while (buffer.hasRemaining() && decodeUnit.getDecodePartEnum() != HttpPartEnum.END) {
                switch (decodeUnit.getDecodePartEnum()) {
                    //解析请求行
                    case REQUEST_LINE_METHOD: {
                        byte b = buffer.get();
                        if (b == Consts.SP) {
                            int p = buffer.position() - 1;
                            buffer.reset();
                            decodeUnit.getHeader().setMethod(MethodEnum.getByMethod(buffer.array(), buffer.position(), p - buffer.position()));
                            buffer.position(p + 1);
                            decodeUnit.setDecodePartEnum(HttpPartEnum.SPACES_BEFORE_URI);
                        }
                        break;
                    }
                    case SPACES_BEFORE_URI: {
                        byte b = buffer.get();
                        if (b != Consts.SP) {
                            buffer.position(buffer.position() - 1);//回滚1位
                            buffer.mark();
                            if (b == '/') {
                                decodeUnit.setDecodePartEnum(HttpPartEnum.AFTER_SLASH_IN_URI);
                                break;
                            }
                            byte c = (byte) (b | 0x20);
                            if (c >= 'a' && c <= 'z') {
                                decodeUnit.setDecodePartEnum(HttpPartEnum.SCHEMA);
                            } else {
                                throw new RuntimeException("decode error");
                            }
                        }
                        break;
                    }
                    case SCHEMA: {
                        byte b = buffer.get();
                        byte c = (byte) (b | 0x20);
                        if (c >= 'a' && c <= 'z') {
                            break;
                        }
                        switch (b) {
                            case ':':
                                int p = buffer.position() - 1;
                                buffer.reset();
                                byte[] b_schema = new byte[p - buffer.position()];
                                buffer.get(b_schema);
                                decodeUnit.getHeader().setB_schema(b_schema);
                                buffer.get();//读取":"
                                decodeUnit.setDecodePartEnum(HttpPartEnum.SCHEMA_SLASH);
                                break;
                            default:
                                throw new RuntimeException("decode error");
                        }
                        break;
                    }
                    case SCHEMA_SLASH: {
                        if (buffer.get() == '/') {
                            decodeUnit.setDecodePartEnum(HttpPartEnum.SCHEMA_SLASH_SLASH);
                        } else {
                            throw new RuntimeException("decode error");
                        }
                        break;
                    }
                    case SCHEMA_SLASH_SLASH: {
                        if (buffer.get() == '/') {
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HOST_START);
                        } else {
                            throw new RuntimeException("decode error");
                        }
                        break;
                    }
                    case HOST_START: {
                        if (buffer.get(buffer.position()) == '[') {
                            throw new RuntimeException("查看nginx源码");
                        } else {
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HOST);
                        }
                        break;
                    }
                    case HOST: {
                        byte b = buffer.get();
                        byte c = (byte) (b | 0x20);
                        if (c >= 'a' && c <= 'z') {
                            break;
                        }
                        if ((b >= '0' && b <= '9') || b == '.' || b == '-') {
                            break;
                        }
                    }
                    case HOST_END: {
                        int p = buffer.position() - 1;
                        buffer.reset();
                        byte[] b_host = new byte[p - buffer.position()];
                        buffer.get(b_host);
                        decodeUnit.getHeader().setB_host(b_host);

                        byte b = buffer.get();
                        buffer.mark();
                        switch (b) {
                            case ':': {
                                decodeUnit.setDecodePartEnum(HttpPartEnum.PORT);
                                break;
                            }
                            case '/': {
                                decodeUnit.setDecodePartEnum(HttpPartEnum.AFTER_SLASH_IN_URI);
                                break;
                            }
                            case ' ': {
                                System.out.println("set uri /");
                                break;
                            }
                            default: {
                                throw new RuntimeException("decode error");
                            }
                        }
                        break;
                    }
                    case HOST_IP_LITERAL: {
                        throw new RuntimeException("查看nginx源码");
                    }
                    case PORT: {
                        byte b = buffer.get();
                        if (b >= '0' && b <= '9') {
                            break;
                        }
                        switch (b) {
                            case '/': {
                                int p = buffer.position() - 1;
                                buffer.reset();
                                byte[] b_port = new byte[p - buffer.position()];
                                buffer.get(b_port);
                                decodeUnit.getHeader().setB_port(b_port);

                                buffer.get();
                                buffer.mark();
                                decodeUnit.setDecodePartEnum(HttpPartEnum.AFTER_SLASH_IN_URI);
                                break;
                            }
                            case ' ': {
                                int p = buffer.position() - 1;
                                buffer.reset();
                                byte[] b_port = new byte[p - buffer.position()];
                                buffer.get(b_port);
                                decodeUnit.getHeader().setB_port(b_port);

                                buffer.get();
                                buffer.mark();
                                System.out.println("set uri /");
                                decodeUnit.setDecodePartEnum(HttpPartEnum.HOST_HTTP_09);
                                break;
                            }
                            default:
                                throw new RuntimeException("查看nginx源码");
                        }
                        break;
                    }
                    case AFTER_SLASH_IN_URI: {
                        byte b = buffer.get();
                        if (b == Consts.SP) {
                            int p = buffer.position() - 1;
                            buffer.reset();
                            byte[] b_uri = new byte[p - buffer.position()];
                            buffer.get(b_uri);
                            decodeUnit.getHeader().setB_uri(b_uri);
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.CHECK_URI_HTTP_09);
                        }
                        break;
                    }
                    case CHECK_URI_HTTP_09: {
                        byte b = buffer.get();
                        switch (b) {
                            case ' ': {
                                buffer.mark();
                                break;
                            }
                            case 'H': {
                                buffer.mark();
                                decodeUnit.setDecodePartEnum(HttpPartEnum.HTTP_H);
                                break;
                            }
                            default:
                                throw new RuntimeException("");
                        }
                        break;
                    }
                    case HTTP_H: {
                        if (buffer.get() == 'T') {
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HTTP_HT);
                        } else {
                            throw new RuntimeException("");
                        }
                        break;
                    }
                    case HTTP_HT: {
                        if (buffer.get() == 'T') {
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HTTP_HTT);
                        } else {
                            throw new RuntimeException("");
                        }
                        break;
                    }
                    case HTTP_HTT: {
                        if (buffer.get() == 'P') {
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HTTP_HTTP);
                        } else {
                            throw new RuntimeException("");
                        }
                        break;
                    }
                    case HTTP_HTTP: {
                        if (buffer.get() == '/') {
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HTTP_VERSION);
                        } else {
                            throw new RuntimeException("");
                        }
                        break;
                    }
                    case HTTP_VERSION: {
                        byte b = buffer.get();
                        if (b == Consts.LF) {
                            int p = buffer.position() - 2;
                            buffer.reset();
                            byte[] b_version = new byte[p - buffer.position()];
                            buffer.get(b_version);
                            decodeUnit.getHeader().setB_http_version(b_version);
                            buffer.get();
                            buffer.get();
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_LINE_KEY);
                        }
                        break;
                    }
                    //解析消息头Key部分
                    case HEAD_LINE_KEY: {
                        while (buffer.hasRemaining()) {
                            byte b = buffer.get();
                            if (b == ' ' || b == ':') {
                                int p = buffer.position() - 1;
                                buffer.reset();
                                decodeUnit.setHeaderKey(new String(buffer.array(), buffer.position(), p - buffer.position()));
                                buffer.position(p);
                                buffer.get();
                                buffer.mark();
                                decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_LINE_VALUE);
                                break;
                            }
                        }
                        break;

                    }  //解析消息头Key部分
                    case HEAD_LINE_VALUE: {
                        boolean has = false;
                        while (buffer.hasRemaining()) {
                            byte b = buffer.get();
                            if (b == Consts.LF) {
                                int p = buffer.position() - 2;
                                buffer.reset();
                                decodeUnit.getHeader().setHeader(decodeUnit.getHeaderKey(), new String(buffer.array(), buffer.position(), p - buffer.position()).trim());
                                buffer.position(p + 2);
                                buffer.mark();
                                decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_END_CHECK);
                                has = true;
                                break;
                            }
                        }
                        if (!has) {
                            buffer.reset();
                            buffer.mark();
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_LINE_VALUE_BUFFER);
                            decodeUnit.getHeadPartDecoder().reset(Consts.CRLF);
                        }
                        break;

                    }
                    case HEAD_LINE_VALUE_BUFFER: {
                        if (decodeUnit.getHeadPartDecoder().decode(buffer)) {
                            buffer.mark();
                            ByteBuffer headLineBuffer = decodeUnit.getHeadPartDecoder().getBuffer();
                            BufferUtils.trim(headLineBuffer);
                            decodeUnit.getHeader().setHeader(decodeUnit.getHeaderKey(), new String(headLineBuffer.array(), headLineBuffer.position(), headLineBuffer.remaining()));
                            //检验Header解码是否结束
                            decodeUnit.setDecodePartEnum(HttpPartEnum.HEAD_END_CHECK);
                        } else {
                            buffer.mark();
                            break;
                        }
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
                        buffer.mark();
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
        } finally {
            //本轮操作未能完成一部分的解码，则还原现场
            if (startPart == decodeUnit.getDecodePartEnum()) {
                buffer.reset();
            }
        }
    }

    /**
     * 获得解码对象模型
     *
     * @param session
     * @param buffer
     * @return
     */
    private HttpDecodeUnit getHttpDecodeUnit(AioSession<HttpRequest> session, ByteBuffer buffer) {
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
            httpHeader.b_headerMap.clear();
            decodeUnit.setDecodePartEnum(HttpPartEnum.REQUEST_LINE_METHOD);
            decodeUnit.getHeadPartDecoder().reset(Consts.SP_ARRAY);
        }
        return decodeUnit;
    }

    @Override
    public ByteBuffer encode(HttpRequest httpRequest, AioSession<HttpRequest> session) {
        return null;
    }

}
