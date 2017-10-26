package org.smartboot.socket.protocol.http;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.protocol.http.strategy.FormWithContentLengthStrategy;
import org.smartboot.socket.protocol.http.strategy.PostDecodeStrategy;
import org.smartboot.socket.protocol.http.strategy.StreamWithContentLengthStrategy;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by 三刀 on 2017/6/20.
 */
public class HttpEntity {

    public static final String AUTHORIZATION = "Authorization";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String DATE = "Date";
    public static final String ETAG = "ETag";
    public static final String EXPIRES = "Expires";
    public static final String HOST = "Host";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String RANGE = "Range";
    public static final String LOCATION = "Location";
    public static final String CONNECTION = "Connection";
    private static final byte[] CRLF = "\r\n\r\n".getBytes();
    private static final String STREAM_BODY = "STREAM_BODY";
    private static final String BLOCK_BODY = "BLOCK_BODY";
    public DelimiterFrameDecoder delimiterFrameDecoder = new DelimiterFrameDecoder(CRLF, 128);
    public FixedLengthFrameDecoder bodyContentDecoder;
    //    public StreamFrameDecoder smartHttpInputStream = new StreamFrameDecoder();
    PostDecodeStrategy postDecodeStrategy;
    private Map<String, PostDecodeStrategy> strategyMap = new HashMap<>();
    /**
     * 0:消息头
     * 1:消息体
     * 2:结束
     */
    private HttpDecodePart decodePart = HttpDecodePart.HEAD;
    private int contentLength = -1;
    private String method, url, protocol, contentType, decodeError;
    private Map<String, String> headMap = new HashMap<String, String>();
    private Map<String, String> paramMap = new HashMap<String, String>();

    {
        strategyMap.put(BLOCK_BODY, new FormWithContentLengthStrategy());
        strategyMap.put(STREAM_BODY, new StreamWithContentLengthStrategy());
    }

    /**
     * 解码HTTP请求头部分
     */
    public void decodeHead() {
        ByteBuffer headBuffer = delimiterFrameDecoder.getBuffer();

        StringTokenizer headerToken = new StringTokenizer(new String(headBuffer.array(), headBuffer.position(), headBuffer.remaining()), "\r\n");

        StringTokenizer requestLineToken = new StringTokenizer(headerToken.nextToken(), " ");
        method = requestLineToken.nextToken();
        url = requestLineToken.nextToken();
        protocol = requestLineToken.nextToken();

        while (headerToken.hasMoreTokens()) {
            StringTokenizer lineToken = new StringTokenizer(headerToken.nextToken(), ":");
            setHeader(lineToken.nextToken().trim(), lineToken.nextToken().trim());
        }

        contentLength = NumberUtils.toInt(headMap.get(CONTENT_LENGTH), -1);
        contentType = headMap.get(CONTENT_TYPE);
        if (StringUtils.equalsIgnoreCase("POST", method) && contentLength != 0) {
            setDecodePart(HttpDecodePart.BODY);
            selectDecodeStrategy();//识别body解码处理器
        } else {
            setDecodePart(HttpDecodePart.END);
        }
        delimiterFrameDecoder = null;
    }

//    public InputStream getInputStream() {
//        return smartHttpInputStream.getInputStream();
//    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
    }

    public Map<String, String> getHeadMap() {
        return headMap;
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDecodeError() {
        return decodeError;
    }

    public void setDecodeError(String decodeError) {
        this.decodeError = decodeError;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    public HttpDecodePart getDecodePart() {
        return decodePart;
    }

    public void setDecodePart(HttpDecodePart decodePart) {
        this.decodePart = decodePart;
    }

    private void selectDecodeStrategy() {
        if (getContentLength() > 0) {
            if (getContentLength() > 0 && StringUtils.startsWith(getContentType(), "application/x-www-form-urlencoded")) {
                postDecodeStrategy = strategyMap.get(BLOCK_BODY);
            } else {
                postDecodeStrategy = strategyMap.get(STREAM_BODY);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
