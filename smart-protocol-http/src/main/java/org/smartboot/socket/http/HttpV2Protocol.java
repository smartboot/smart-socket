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
import org.smartboot.socket.http.strategy.BodyTypeEnum;
import org.smartboot.socket.http.strategy.FormWithContentLengthStrategy;
import org.smartboot.socket.http.strategy.PostDecodeStrategy;
import org.smartboot.socket.http.strategy.StreamWithContentLengthStrategy;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
public class HttpV2Protocol implements Protocol<HttpV2Entity> {
    private static final Logger LOGGER = LogManager.getLogger(HttpV2Protocol.class);
    private static final byte[] LINE_END_BYTES = {'\r', '\n'};
    private static final String HTTP_ENTITY = "_http_entity_";
    private static final String STREAM_BODY = "STREAM_BODY";
    private static final String BLOCK_BODY = "BLOCK_BODY";
    private Map<String, PostDecodeStrategy> strategyMap = new HashMap<>();

    {
        strategyMap.put(BLOCK_BODY, new FormWithContentLengthStrategy());
        strategyMap.put(STREAM_BODY, new StreamWithContentLengthStrategy());
    }

    @Override
    public HttpV2Entity decode(ByteBuffer buffer, AioSession<HttpV2Entity> session, boolean eof) {

        HttpDecodeUnit decodeUnit = null;
        if (session.getAttachment() == null) {
            decodeUnit = new HttpDecodeUnit();
            decodeUnit.entity = new HttpV2Entity(session);
            decodeUnit.partEnum = HttpPartEnum.REQUEST_LINE;
            decodeUnit.delimiterFrameDecoder = new DelimiterFrameDecoder(LINE_END_BYTES, 256);
            session.setAttachment(decodeUnit);
        } else {
            decodeUnit = (HttpDecodeUnit) session.getAttachment();
        }
        HttpV2Entity entity = decodeUnit.entity;

        boolean returnEntity = false;//是否返回HttpEntity
        boolean continueDecode = true;//是否继续读取数据
        while (buffer.hasRemaining() && continueDecode) {
            switch (decodeUnit.partEnum) {
                //解析请求行
                case REQUEST_LINE:
                    if (decodeUnit.delimiterFrameDecoder.decode(buffer)) {
                        decodeRequestLine(decodeUnit);
                    }
                    break;
                //解析消息头
                case HEAD_LINE:
                    if (decodeUnit.delimiterFrameDecoder.decode(buffer)) {
                        decodeHeadLine(decodeUnit);
                    }
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

    @Override
    public ByteBuffer encode(HttpV2Entity httpEntity, AioSession<HttpV2Entity> session) {
        return null;
    }

    /**
     * 解析请求行
     *
     * @param unit
     */
    private void decodeRequestLine(HttpDecodeUnit unit) {
        ByteBuffer requestLineBuffer = unit.delimiterFrameDecoder.getBuffer();
        String[] requestLineDatas = StringUtils.split(new String(requestLineBuffer.array(), 0, requestLineBuffer.remaining()), " ");

        unit.entity.setMethod(requestLineDatas[0]);
        unit.entity.setUrl(requestLineDatas[1]);
        unit.entity.setProtocol(requestLineDatas[2]);

        //识别一下一个解码阶段
        unit.delimiterFrameDecoder.reset();
        unit.partEnum = HttpPartEnum.HEAD_LINE;
    }

    /**
     * 解析消息头
     *
     * @param unit
     */
    private void decodeHeadLine(HttpDecodeUnit unit) {
        ByteBuffer headLineBuffer = unit.delimiterFrameDecoder.getBuffer();

        //消息头已结束
        if (headLineBuffer.remaining() == LINE_END_BYTES.length) {
            unit.partEnum = HttpPartEnum.HEAD_END_LINE;
            unit.delimiterFrameDecoder.reset();
            return;
        }
        String[] headLineDatas = StringUtils.split(new String(headLineBuffer.array(), 0, headLineBuffer.remaining()), ":");

        unit.entity.setHeader(headLineDatas[0], headLineDatas[1]);

        //识别一下一个解码阶段
        unit.delimiterFrameDecoder.reset();
        unit.partEnum = HttpPartEnum.HEAD_LINE;
    }

    private void decodeHeadEnd(HttpDecodeUnit unit) {

        if (!StringUtils.equals("POST", unit.entity.getMethod())) {
            unit.partEnum = HttpPartEnum.END;
            return;


        }
        unit.partEnum = HttpPartEnum.BODY;
        //识别Body解码器
        String contentType = unit.entity.getHeader(HttpV2Entity.CONTENT_TYPE);
        int contentLength = unit.entity.getContentLength();
        if (StringUtils.startsWith(contentType, "multipart/form-data")) {
            unit.bodyTypeEnum = BodyTypeEnum.STREAM;
            unit.streamFrameDecoder = new StreamFrameDecoder(contentLength);
            unit.entity.setInputStream(unit.streamFrameDecoder.getInputStream());
        } else {
            unit.bodyTypeEnum = BodyTypeEnum.FORM;
            unit.fixedLengthFrameDecoder = new FixedLengthFrameDecoder(contentLength);
        }
    }

    private void decodeBody(HttpDecodeUnit unit, ByteBuffer buffer) {
        switch (unit.bodyTypeEnum) {
            case FORM:
                if (unit.fixedLengthFrameDecoder.decode(buffer)) {

                }
                break;
            case STREAM:
                if (unit.streamFrameDecoder.decode(buffer)) {

                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void selectDecodeStrategy(HttpV2Entity entity) {
        if (entity.getContentLength() > 0) {
            if (entity.getContentLength() > 0 && StringUtils.startsWith(entity.getContentType(), "application/x-www-form-urlencoded")) {
                entity.postDecodeStrategy = strategyMap.get(BLOCK_BODY);
            } else {
                entity.postDecodeStrategy = strategyMap.get(STREAM_BODY);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        LOGGER.info(entity.postDecodeStrategy);
    }

    class HttpDecodeUnit {


        HttpV2Entity entity;
        /**
         * 当前解码阶段
         */
        HttpPartEnum partEnum;

        BodyTypeEnum bodyTypeEnum;
        /**
         * 结束标解码器
         */
        DelimiterFrameDecoder delimiterFrameDecoder;

        FixedLengthFrameDecoder fixedLengthFrameDecoder;

        StreamFrameDecoder streamFrameDecoder;
    }
}
