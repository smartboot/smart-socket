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
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
public class HttpProtocol implements Protocol<HttpEntity> {
    private static final Logger LOGGER = LogManager.getLogger(HttpProtocol.class);
    private static final byte[] LINE_END_BYTES = {'\r', '\n'};

    @Override
    public HttpEntity decode(ByteBuffer buffer, AioSession<HttpEntity> session, boolean eof) {

        HttpDecodeUnit decodeUnit = null;
        if (session.getAttachment() == null) {
            decodeUnit = new HttpDecodeUnit();
            decodeUnit.entity = new HttpEntity(session);
            decodeUnit.partEnum = HttpPartEnum.REQUEST_LINE;
            decodeUnit.delimiterFrameDecoder = new DelimiterFrameDecoder(LINE_END_BYTES, 256);
            session.setAttachment(decodeUnit);
        } else {
            decodeUnit = (HttpDecodeUnit) session.getAttachment();
        }
        HttpEntity entity = decodeUnit.entity;

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
    public ByteBuffer encode(HttpEntity httpEntity, AioSession<HttpEntity> session) {
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
        String contentType = unit.entity.getHeader(HttpEntity.CONTENT_TYPE);
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
                    decodeBodyForm(unit);
                    unit.partEnum = HttpPartEnum.END;
                }
                break;
            case STREAM:
                if (unit.streamFrameDecoder.decode(buffer)) {
                    unit.partEnum = HttpPartEnum.END;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void decodeBodyForm(HttpDecodeUnit unit) {
        ByteBuffer buffer = unit.fixedLengthFrameDecoder.getBuffer();
        String[] paramArray = StringUtils.split(new String(buffer.array(), buffer.position(), buffer.remaining()), "&");
        for (int i = 0; i < paramArray.length; i++) {
            unit.entity.setParam(StringUtils.substringBefore(paramArray[i], "=").trim(), StringUtils.substringAfter(paramArray[i], "=").trim());
        }
    }

    class HttpDecodeUnit {


        HttpEntity entity;
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
