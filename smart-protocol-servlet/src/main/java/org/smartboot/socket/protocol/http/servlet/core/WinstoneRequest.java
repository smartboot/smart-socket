/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.extension.decoder.StreamFrameDecoder;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.protocol.http.HttpDecodePart;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationPrincipal;
import org.smartboot.socket.protocol.http.strategy.FormWithContentLengthStrategy;
import org.smartboot.socket.protocol.http.strategy.PostDecodeStrategy;
import org.smartboot.socket.protocol.http.strategy.StreamWithContentLengthStrategy;
import org.smartboot.socket.protocol.http.util.SizeRestrictedHashMap;
import org.smartboot.socket.protocol.http.util.SizeRestrictedHashtable;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * Implements the request interface required by the servlet spec.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneRequest.java,v 1.40 2008/10/01 14:46:13 rickknowles Exp
 * $
 */
public class WinstoneRequest implements HttpServletRequest {

    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    protected static final DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    protected static final Random rnd;
    private static final byte[] CRLF = "\r\n\r\n".getBytes();
    private static final String STREAM_BODY = "STREAM_BODY";
    private static final String BLOCK_BODY = "BLOCK_BODY";
    protected static Logger logger = LogManager.getLogger(WinstoneRequest.class);
    private static Map<String, PostDecodeStrategy> strategyMap = new HashMap<>();

    static {
        WinstoneRequest.headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
        rnd = new Random(System.currentTimeMillis());
    }

    private final Set<WinstoneSession> usedSessions;
    /**
     * max number of parameters allowed.
     */
    private final int maxParamAllowed;
    public DelimiterFrameDecoder delimiterFrameDecoder = new DelimiterFrameDecoder(CRLF, 128);
    public FixedLengthFrameDecoder bodyContentDecoder;
    public StreamFrameDecoder smartHttpInputStream = new StreamFrameDecoder(1024);
    public PostDecodeStrategy postDecodeStrategy;
    protected Map<String, Object> attributes;
    protected Map<String, String[]> parameters;
    // protected Map forwardedParameters;
    protected Cookie cookies[];
    protected String scheme;
    protected String serverName;
    protected String requestURI;
    protected String servletPath;
    protected String pathInfo;
    protected String queryString;
    protected String encoding;
    protected int serverPort;
    protected String remoteIP;
    protected String remoteName;
    protected int remotePort;
    protected String localAddr;
    protected String localName;
    protected int localPort;
    /**
     * If Boolean.TRUE, it indicates that the request body was already consumed
     * because of the call to {@link #getParameterMap()} (or its sibling), which
     * requires implicit form parameter parsing.
     * <p>
     * If Boolean.FALSE, it indicates that the request body shall not be
     * consumed by the said method, because the application already called
     * {@link #getInputStream()} and showed the intent to parse the request body
     * on its own.
     * <p>
     * If null, it indicates that we haven't come to that decision.
     */
    protected Boolean parsedParameters;
    protected Map<String, String> requestedSessionIds;
    protected Map<String, String> currentSessionIds;
    protected String deadRequestedSessionId;
    protected List<Locale> locales;
    protected String authorization;
    protected boolean isSecure;
    protected WinstoneInputStream inputData;
    protected BufferedReader inputReader;
    protected ServletConfiguration servletConfig;
    protected WebAppConfiguration webappConfig;
    protected HostGroup hostGroup;
    protected AuthenticationPrincipal authenticatedUser;
    protected ServletRequestAttributeListener requestAttributeListeners[];
    protected ServletRequestListener requestListeners[];
    private Http11Request http11Request;
    private MessageDigest md5Digester;
    /**
     * 0:消息头
     * 1:消息体
     * 2:结束
     */
    private HttpDecodePart decodePart = HttpDecodePart.HEAD;
    private int contentLength = -1;
    private String method, url, protocol, contentType, decodeError;
    private Map<String, String> headMap = new HashMap<String, String>();

    {
        strategyMap.put(BLOCK_BODY, new FormWithContentLengthStrategy());
        strategyMap.put(STREAM_BODY, new StreamWithContentLengthStrategy());
    }

    /**
     * Build a new instance of WinstoneRequest.
     *
     * @throws IOException
     */
    public WinstoneRequest(final int maxParamAllowed, Http11Request request) throws IOException {
        super();
        this.maxParamAllowed = maxParamAllowed < 1 ? WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED : maxParamAllowed;
        attributes = new HashMap<String, Object>();
        parameters = new SizeRestrictedHashtable<String, String[]>(this.maxParamAllowed);
        locales = new ArrayList<Locale>();
        // this.forwardedParameters = new Hashtable();
        requestedSessionIds = new HashMap<String, String>();
        currentSessionIds = new HashMap<String, String>();
        usedSessions = new HashSet<WinstoneSession>();
        contentLength = -1;
        isSecure = Boolean.FALSE;
        try {
            md5Digester = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException err) {
            throw new WinstoneException("MD5 digester unavailable - what the ...?");
        }
        this.http11Request = request;
    }

    /**
     * Gets parameters from the url encoded parameter string
     */
    public static void extractParameters(final String urlEncodedParams, final String encoding, final Map<String, String[]> outputParams, final boolean overwrite) {
        WinstoneRequest.logger.debug("Parsing parameters: {} (using encoding {})", urlEncodedParams, encoding);
        final StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", Boolean.FALSE);
        Set<String> overwrittenParamNames = null;
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            final int equalPos = token.indexOf('=');
            try {
                final String decodedName = decodeURLToken(equalPos == -1 ? token : token.substring(0, equalPos), encoding == null ? "UTF-8" : encoding);
                final String decodedValue = (equalPos == -1 ? "" : decodeURLToken(token.substring(equalPos + 1), encoding == null ? "UTF-8" : encoding));

                String[] already = null;
                if (overwrite) {
                    if (overwrittenParamNames == null) {
                        overwrittenParamNames = new HashSet<String>();
                    }
                    if (!overwrittenParamNames.contains(decodedName)) {
                        overwrittenParamNames.add(decodedName);
                        outputParams.remove(decodedName);
                    }
                }
                already = outputParams.get(decodedName);
                if (already == null) {
                    outputParams.put(decodedName, new String[]{decodedValue});
                } else {
                    final String alreadyArray[] = already;
                    final String oneMore[] = new String[alreadyArray.length + 1];
                    System.arraycopy(alreadyArray, 0, oneMore, 0, alreadyArray.length);
                    oneMore[oneMore.length - 1] = decodedValue;
                    outputParams.put(decodedName, oneMore);
                }
            } catch (final UnsupportedEncodingException err) {
                WinstoneRequest.logger.error("Error parsing request parameters", err);
            }
        }
    }

    /**
     * For decoding the URL encoding used on query strings as "UTF-8".
     *
     * @param in input string
     * @return decoded string
     */
    public static String decodeURLToken(String in) {
        try {
            return decodeURLToken(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(); // impossible
        }
    }

    /**
     * For decoding the URL encoding used on query strings
     *
     * @param in       input string
     * @param encoding encoding
     * @return decoded string
     * @throws UnsupportedEncodingException
     */
    public static String decodeURLToken(String in, String encoding) throws UnsupportedEncodingException {
        return URLDecoder.decode(in, encoding);
    }

    /**
     * For decoding the URL encoding using UTF-8.
     * Decode as path token, where '+' is not an escape character.
     *
     * @param in in input string
     * @return decoded string
     * @throws UnsupportedEncodingException
     */
    public static String decodePathURLToken(String in) {
        try {
            return decodePathURLToken(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(); // impossible
        }
    }

    /**
     * For decoding the URL encoding.
     * Decode as path token, where '+' is not an escape character.
     *
     * @param in       in input string
     * @param encoding encoding
     * @return decoded string
     * @throws UnsupportedEncodingException
     */
    public static String decodePathURLToken(String in, String encoding) throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int n = 0; n < in.length(); n++) {
            char thisChar = in.charAt(n);
            if (thisChar == '%') {
                String token = in.substring(Math.min(n + 1, in.length()), Math.min(n + 3, in.length()));
                try {
                    int decoded = Integer.parseInt(token, 16);
                    baos.write(decoded);
                    n += 2;
                } catch (RuntimeException err) {
                    WinstoneRequest.logger.warn("Found an invalid character %{} in url parameter. Echoing through in escaped form", token);
                    baos.write(thisChar);
                }
            } else
                baos.write(thisChar);
        }
        return new String(baos.toByteArray(), encoding);
    }

    private static String nextToken(final StringTokenizer st) {
        if (st.hasMoreTokens()) {
            return st.nextToken();
        } else {
            return null;
        }
    }

    private static String extractFromQuotes(final String input) {
        if ((input != null) && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
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
        requestURI = trimHostName(url);
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

    private String trimHostName(final String input) {
        if (input == null) {
            return null;
        } else if (input.startsWith("/")) {
            return input;
        }

        final int hostStart = input.indexOf("://");
        if (hostStart == -1) {
            return input;
        }
        final String hostName = input.substring(hostStart + 3);
        final int pathStart = hostName.indexOf('/');
        if (pathStart == -1) {
            return "/";
        } else {
            return hostName.substring(pathStart);
        }
    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
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

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        requestListeners = null;
        requestAttributeListeners = null;
        attributes.clear();
        parameters.clear();
        // this.forwardedParameters.clear();
        usedSessions.clear();
        cookies = null;
        method = null;
        scheme = null;
        serverName = null;
        requestURI = null;
        servletPath = null;
        pathInfo = null;
        queryString = null;
        protocol = null;
        contentLength = -1;
        contentType = null;
        encoding = null;
        inputData = null;
        inputReader = null;
        servletConfig = null;
        webappConfig = null;
        hostGroup = null;
        serverPort = -1;
        remoteIP = null;
        remoteName = null;
        remotePort = -1;
        localAddr = null;
        localName = null;
        localPort = -1;
        parsedParameters = null;
        requestedSessionIds.clear();
        currentSessionIds.clear();
        deadRequestedSessionId = null;
        locales.clear();
        authorization = null;
        isSecure = Boolean.FALSE;
        authenticatedUser = null;
    }


    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public Map<String, String> getCurrentSessionIds() {
        return currentSessionIds;
    }

    public void setCurrentSessionIds(final Map<String, String> currentSessionIds) {
        this.currentSessionIds = currentSessionIds;
    }

    public Map<String, String> getRequestedSessionIds() {
        return requestedSessionIds;
    }

    public void setRequestedSessionIds(final Map<String, String> requestedSessionIds) {
        this.requestedSessionIds = requestedSessionIds;
    }

    public String getDeadRequestedSessionId() {
        return deadRequestedSessionId;
    }

    public void setDeadRequestedSessionId(final String deadRequestedSessionId) {
        this.deadRequestedSessionId = deadRequestedSessionId;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(final HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public WebAppConfiguration getWebAppConfig() {
        return webappConfig;
    }

    public void setWebAppConfig(final WebAppConfiguration webappConfig) {
        this.webappConfig = webappConfig;
    }

    public ServletConfiguration getServletConfig() {
        return servletConfig;
    }

    public void setServletConfig(final ServletConfiguration servletConfig) {
        this.servletConfig = servletConfig;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public Boolean getParsedParameters() {
        return parsedParameters;
    }

    public void setParsedParameters(final Boolean parsed) {
        parsedParameters = parsed;
    }

    public List<Locale> getListLocales() {
        return locales;
    }

    public void setRemoteIP(final String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public void setRemoteName(final String name) {
        remoteName = name;
    }

    public void setIsSecure(final boolean isSecure) {
        this.isSecure = isSecure;
    }

    public void setAuthorization(final String auth) {
        authorization = auth;
    }

    public void setRequestListeners(final ServletRequestListener rl[]) {
        requestListeners = rl;
    }

    public void setRequestAttributeListeners(final ServletRequestAttributeListener ral[]) {
        requestAttributeListeners = ral;
    }

    public void discardRequestBody() {
        if (getContentLength() > 0) {
            try {
                WinstoneRequest.logger.debug("Forcing request body parse");

                // if there's any input that we haven't consumed, throw them
                // away, so that the next request parsing
                // will start from the right position.
                byte buffer[] = new byte[2048];
                while ((this.inputData.read(buffer)) != -1)
                    ;
            } catch (IOException err) {
                WinstoneRequest.logger.error("Forcing request body parse", err);
            }
        }
    }

    /**
     * This takes the parameters in the body of the request and puts them into
     * the parameters map.
     */
    public void parseRequestParameters() {
        if ((parsedParameters != null) && !parsedParameters.booleanValue()) {
            WinstoneRequest.logger.warn("Called getInputStream after getParameter ... error");
            parsedParameters = Boolean.TRUE;
        } else if (parsedParameters == null) {
            final Map<String, String[]> workingParameters = new SizeRestrictedHashMap<String, String[]>(maxParamAllowed);
            try {
                // Parse query string from request
                // if ((method.equals(METHOD_GET) || method.equals(METHOD_HEAD)
                // ||
                // method.equals(METHOD_POST)) &&
                if (queryString != null) {
                    WinstoneRequest.extractParameters(queryString, encoding, workingParameters, Boolean.FALSE);
                    WinstoneRequest.logger.debug("Param line: " + workingParameters);
                }

                if (method.equals(WinstoneConstant.METHOD_POST) && (contentType != null) && (contentType.equals(WinstoneConstant.POST_PARAMETERS) || contentType.startsWith(WinstoneConstant.POST_PARAMETERS + ";"))) {
                    WinstoneRequest.logger.debug("Parsing request body for parameters");

                    // Parse params
                    final byte paramBuffer[] = new byte[contentLength];
                    int readCount = this.inputData.readAsMuchAsPossible(paramBuffer, 0, contentLength);
                    if (readCount != contentLength) {
                        WinstoneRequest.logger.warn("Content-length said {}, actual length was {}", Integer.toString(contentLength), Integer.toString(readCount));
                    }
                    final String paramLine = (encoding == null ? new String(paramBuffer) : new String(paramBuffer, encoding));
                    WinstoneRequest.extractParameters(paramLine.trim(), encoding, workingParameters, Boolean.FALSE);
                    WinstoneRequest.logger.debug("Param line: " + workingParameters.toString());
                }

                parameters.putAll(workingParameters);
                parsedParameters = Boolean.TRUE;
            } catch (final Throwable err) {
                WinstoneRequest.logger.error("Error parsing body of the reques", err);
                parsedParameters = null;
            }
        }
    }


    private void parseCookieLine(final String headerValue, final List<Cookie> cookieList) {
        final StringTokenizer st = new StringTokenizer(headerValue, ";", Boolean.FALSE);
        int version = 0;
        String cookieLine = WinstoneRequest.nextToken(st);

        // check cookie version flag
        if ((cookieLine != null) && cookieLine.startsWith("$Version=")) {
            final int equalPos = cookieLine.indexOf('=');
            try {
                version = Integer.parseInt(WinstoneRequest.extractFromQuotes(cookieLine.substring(equalPos + 1).trim()));
            } catch (final NumberFormatException err) {
                version = 0;
            }
            cookieLine = WinstoneRequest.nextToken(st);
        }

        // process remainder - parameters
        while (cookieLine != null) {
            cookieLine = cookieLine.trim();
            int equalPos = cookieLine.indexOf('=');
            if (equalPos == -1) {
                // next token
                cookieLine = WinstoneRequest.nextToken(st);
            } else {
                final String name = cookieLine.substring(0, equalPos);
                final String value = WinstoneRequest.extractFromQuotes(cookieLine.substring(equalPos + 1));
                final Cookie thisCookie = new Cookie(name, value);
                thisCookie.setVersion(version);
                thisCookie.setSecure(isSecure());
                cookieList.add(thisCookie);

                // check for path / domain / port
                cookieLine = WinstoneRequest.nextToken(st);
                while ((cookieLine != null) && cookieLine.trim().startsWith("$")) {
                    cookieLine = cookieLine.trim();
                    equalPos = cookieLine.indexOf('=');
                    final String attrValue = equalPos == -1 ? "" : cookieLine.substring(equalPos + 1).trim();
                    if (cookieLine.startsWith("$Path")) {
                        thisCookie.setPath(WinstoneRequest.extractFromQuotes(attrValue));
                    } else if (cookieLine.startsWith("$Domain")) {
                        thisCookie.setDomain(WinstoneRequest.extractFromQuotes(attrValue));
                    }
                    cookieLine = WinstoneRequest.nextToken(st);
                }
                WinstoneRequest.logger.debug("Found cookie: " + thisCookie.toString());
                if (thisCookie.getName().equals(WinstoneSession.SESSION_COOKIE_NAME)) {
                    // Find a context that manages this key
                    final HostConfiguration hostConfig = hostGroup.getHostByName(serverName);
                    final WebAppConfiguration ownerContext = hostConfig.getWebAppBySessionKey(thisCookie.getValue());
                    if (ownerContext != null) {
                        requestedSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
                        currentSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
                    } // If not found, it was probably dead
                    else {
                        deadRequestedSessionId = thisCookie.getValue();
                    }
                    // this.requestedSessionId = thisCookie.getValue();
                    // this.currentSessionId = thisCookie.getValue();
                    WinstoneRequest.logger.debug("Found session cookie: {} {}", thisCookie.getValue(), ownerContext == null ? "" : "prefix:" + ownerContext.getContextPath());
                }
            }
        }
    }

    private List<Locale> parseLocales(final String header) {
        // Strip out the whitespace
        final StringBuilder lb = new StringBuilder();
        for (int n = 0; n < header.length(); n++) {
            final char c = header.charAt(n);
            if (!Character.isWhitespace(c)) {
                lb.append(c);
            }
        }

        // Tokenize by commas
        final Map<Float, List<Locale>> localeEntries = new SizeRestrictedHashMap<Float, List<Locale>>(maxParamAllowed);
        final StringTokenizer commaTK = new StringTokenizer(lb.toString(), ",", Boolean.FALSE);
        for (; commaTK.hasMoreTokens(); ) {
            String clause = commaTK.nextToken();

            // Tokenize by semicolon
            Float quality = new Float(1);
            if (clause.indexOf(";q=") != -1) {
                final int pos = clause.indexOf(";q=");
                try {
                    quality = new Float(clause.substring(pos + 3));
                } catch (final NumberFormatException err) {
                    quality = new Float(0);
                }
                clause = clause.substring(0, pos);
            }

            // Build the locale
            String language = "";
            String country = "";
            String variant = "";
            final int dpos = clause.indexOf('-');
            if (dpos == -1) {
                language = clause;
            } else {
                language = clause.substring(0, dpos);
                final String remainder = clause.substring(dpos + 1);
                final int d2pos = remainder.indexOf('-');
                if (d2pos == -1) {
                    country = remainder;
                } else {
                    country = remainder.substring(0, d2pos);
                    variant = remainder.substring(d2pos + 1);
                }
            }
            final Locale loc = new Locale(language, country, variant);

            // Put into list by quality
            List<Locale> localeList = localeEntries.get(quality);
            if (localeList == null) {
                localeList = new ArrayList<Locale>();
                localeEntries.put(quality, localeList);
            }
            localeList.add(loc);
        }

        // Extract and build the list
        final Float orderKeys[] = localeEntries.keySet().toArray(new Float[0]);
        Arrays.sort(orderKeys);
        final List<Locale> outputLocaleList = new ArrayList<Locale>();
        for (int n = 0; n < orderKeys.length; n++) {
            // Skip backwards through the list of maps and add to the output
            // list
            final int reversedIndex = (orderKeys.length - 1) - n;
            if ((orderKeys[reversedIndex].floatValue() <= 0) || (orderKeys[reversedIndex].floatValue() > 1)) {
                continue;
            }
            final List<Locale> localeList = localeEntries.get(orderKeys[reversedIndex]);
            for (final Iterator<Locale> i = localeList.iterator(); i.hasNext(); ) {
                outputLocaleList.add(i.next());
            }
        }

        return outputLocaleList;
    }


    public void setForwardQueryString(final String forwardQueryString) {
        // this.forwardedParameters.clear();

        // Parse query string from include / forward
        if (forwardQueryString != null) {
            final String oldQueryString = queryString == null ? "" : queryString;
            final boolean needJoiner = !forwardQueryString.equals("") && !oldQueryString.equals("");
            queryString = forwardQueryString + (needJoiner ? "&" : "") + oldQueryString;

            if (parsedParameters != null) {
                WinstoneRequest.logger.debug("Parsing parameters: {} (using encoding {})", forwardQueryString, encoding);
                WinstoneRequest.extractParameters(forwardQueryString, encoding, parameters, Boolean.TRUE);
                WinstoneRequest.logger.debug("Param line: {}", parameters != null ? parameters.toString() : "");
            }
        }

    }

    // Implementation methods for the servlet request stuff
    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void removeAttribute(final String name) {
        final Object value = attributes.get(name);
        if (value == null) {
            return;
        }

        // fire event
        if (requestAttributeListeners != null) {
            for (int n = 0; n < requestAttributeListeners.length; n++) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                requestAttributeListeners[n].attributeRemoved(new ServletRequestAttributeEvent(webappConfig, this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        attributes.remove(name);
    }

    @Override
    public void setAttribute(final String name, final Object o) {
        if ((name != null) && (o != null)) {
            final Object oldValue = attributes.get(name);
            attributes.put(name, o); // make sure it's set at the top level

            // fire event
            if (requestAttributeListeners != null) {
                if (oldValue == null) {
                    for (int n = 0; n < requestAttributeListeners.length; n++) {
                        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        requestAttributeListeners[n].attributeAdded(new ServletRequestAttributeEvent(webappConfig, this, name, o));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                } else {
                    for (int n = 0; n < requestAttributeListeners.length; n++) {
                        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        requestAttributeListeners[n].attributeReplaced(new ServletRequestAttributeEvent(webappConfig, this, name, oldValue));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                }
            }
        } else if (name != null) {
            removeAttribute(name);
        }
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
        "blah".getBytes(encoding); // throws an exception if the encoding is
        // unsupported
        if (inputReader == null) {
            WinstoneRequest.logger.debug("Setting the request encoding from {} to {}", this.encoding, encoding);
            this.encoding = encoding;
        }
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(final int len) {
        contentLength = len;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String type) {
        contentType = type;
    }

    @Override
    public Locale getLocale() {
        return locales.isEmpty() ? Locale.getDefault() : (Locale) locales.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        final List<Locale> sendLocales = locales;
        if (sendLocales.isEmpty()) {
            sendLocales.add(Locale.getDefault());
        }
        return Collections.enumeration(sendLocales);
    }

    public void setLocales(final List<Locale> locales) {
        this.locales = locales;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocolString) {
        protocol = protocolString;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (inputReader != null) {
            return inputReader;
        } else {
            if (parsedParameters != null) {
                if (parsedParameters.equals(Boolean.TRUE)) {
                    WinstoneRequest.logger.warn("Called getReader after getParameter ... error");
                } else {
                    throw new IllegalStateException("Called getReader() after getInputStream() on request");
                }
            }
            if (encoding != null) {
                inputReader = new BufferedReader(new InputStreamReader(inputData, encoding));
            } else {
                inputReader = new BufferedReader(new InputStreamReader(inputData));
            }
            parsedParameters = Boolean.FALSE;
            return inputReader;
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (inputReader != null) {
            throw new IllegalStateException("Called getInputStream() after getReader() on request");
        }
        if (parsedParameters != null) {
            if (parsedParameters.equals(Boolean.TRUE)) {
                WinstoneRequest.logger.warn("Called getInputStream after getParameter ... error");
            }
        }
        if (method.equals(WinstoneConstant.METHOD_POST) && WinstoneConstant.POST_PARAMETERS.equals(contentType)) {
            this.parsedParameters = Boolean.FALSE;
        }
        return inputData;
    }

    public void setInputStream(final WinstoneInputStream inputData) {
        this.inputData = inputData;
    }

    @Override
    public String getParameter(final String name) {
        String[] param = parameters.get(name);
        if (param == null) {
            return null;
        } else {
            return param[0];
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(final String name) {
        return parameters.get(name);
    }

    @Override
    public Map<String, Object> getParameterMap() {
        final Hashtable<String, Object> paramMap = new SizeRestrictedHashtable<String, Object>(maxParamAllowed);
        for (final Enumeration<String> names = getParameterNames(); names.hasMoreElements(); ) {
            final String name = names.nextElement();
            paramMap.put(name, getParameterValues(name));
        }
        return paramMap;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    public void setServerName(final String name) {
        serverName = name;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(final int port) {
        serverPort = port;
    }

    @Override
    public String getRemoteAddr() {
        return remoteIP;
    }

    @Override
    public String getRemoteHost() {
        return remoteName;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(final int port) {
        remotePort = port;
    }

    @Override
    public String getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(final String ip) {
        localName = ip;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    public void setLocalName(final String name) {
        localName = name;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(final int port) {
        localPort = port;
    }

    /**
     * 为了允许使用相对于当前请求路径的相对路径(不是相对于 ServletContext 根路径)获得 RequestDispatcher 对象，
     * 在 ServletRequest 接口中 供了 getRequestDispatcher 方法。
     * 此方法的行为与 ServletContext 中同名的方法相似。
     * Servlet 容器根据 request 对象中的信息把给定的相对路 径转换成当前 servlet 的完整路径。
     * 例如，在以‟/‟作为上下文根路径和请求路径/garden/tools.html 中，
     * 通过 ServletRequest.getRequestDispatcher("header.html") 获得的请求分派器和通过调用ServletContext.getRequestDispatcher("/garden/header.html")获得的完全一样。
     *
     * @param path
     * @return
     */
    @Override
    public javax.servlet.RequestDispatcher getRequestDispatcher(final String path) {
        if (path.startsWith("/")) {
            return webappConfig.getRequestDispatcher(path);
        }

        // Take the servlet path + pathInfo, and make an absolute path
        final String fullPath = getServletPath() + (getPathInfo() == null ? "" : getPathInfo());
        final int lastSlash = fullPath.lastIndexOf('/');
        final String currentDir = (lastSlash == -1 ? "/" : fullPath.substring(0, lastSlash + 1));
        return webappConfig.getRequestDispatcher(currentDir + path);
    }

    // Now the stuff for HttpServletRequest
    @Override
    public String getContextPath() {
        return webappConfig.getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public long getDateHeader(final String name) {
        final String dateHeader = getHeader(name);
        if (dateHeader == null) {
            return -1;
        } else {
            try {
                Date date = null;
                synchronized (WinstoneRequest.headerDF) {
                    date = WinstoneRequest.headerDF.parse(dateHeader);
                }
                return date.getTime();
            } catch (final java.text.ParseException err) {
                throw new IllegalArgumentException("Can't convert to date - " + dateHeader);
            }
        }
    }

    @Override
    public int getIntHeader(final String name) {
        final String header = getHeader(name);
        return header == null ? -1 : Integer.parseInt(header);
    }

    @Override
    public String getHeader(final String name) {
        return headMap.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headMap.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return Collections.enumeration(headMap.keySet());
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return webappConfig.getRealPath(pathInfo);
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }

    @Override
    public String getRequestURI() {
        return http11Request.getRequestURI();
    }

    public void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(final String servletPath) {
        this.servletPath = servletPath;
    }

    @Override
    public String getRequestedSessionId() {
        final String actualSessionId = requestedSessionIds.get(webappConfig.getContextPath());
        if (actualSessionId != null) {
            return actualSessionId;
        } else {
            return deadRequestedSessionId;
        }
    }

    @Override
    public StringBuffer getRequestURL() {
        final StringBuffer url = new StringBuffer();
        url.append(getScheme()).append("://");
        url.append(getServerName());
        if (!((getServerPort() == 80) && getScheme().equals("http")) && !((getServerPort() == 443) && getScheme().equals("https"))) {
            url.append(':').append(getServerPort());
        }
        url.append(getRequestURI()); // need encoded form, so can't use servlet
        // path + path info
        return url;
    }

    @Override
    public Principal getUserPrincipal() {
        return authenticatedUser;
    }

    @Override
    public boolean isUserInRole(final String role) {
        if (authenticatedUser == null) {
            return Boolean.FALSE;
        } else if (servletConfig.getSecurityRoleRefs() == null) {
            return authenticatedUser.isUserIsInRole(role);
        } else {
            final String replacedRole = servletConfig.getSecurityRoleRefs().get(role);
            return authenticatedUser.isUserIsInRole(replacedRole == null ? role : replacedRole);
        }
    }

    @Override
    public String getAuthType() {
        return authenticatedUser == null ? null : authenticatedUser.getAuthType();
    }

    @Override
    public String getRemoteUser() {
        return authenticatedUser == null ? null : authenticatedUser.getName();
    }

    public void setRemoteUser(final AuthenticationPrincipal user) {
        authenticatedUser = user;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return (getRequestedSessionId() != null);
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        final String requestedId = getRequestedSessionId();
        if (requestedId == null) {
            return Boolean.FALSE;
        }
        final WinstoneSession ws = webappConfig.getSessionById(requestedId, Boolean.FALSE);
        return (ws != null);
    }

    @Override
    public HttpSession getSession() {
        return getSession(Boolean.TRUE);
    }

    @Override
    public HttpSession getSession(final boolean create) {
        String cookieValue = currentSessionIds.get(webappConfig.getContextPath());

        // Handle the null case
        if (cookieValue == null) {
            if (!create) {
                return null;
            } else {
                cookieValue = makeNewSession().getId();
            }
        }

        // Now get the session object
        WinstoneSession session = webappConfig.getSessionById(cookieValue, Boolean.FALSE);
        if (create && (session == null)) {
            session = makeNewSession();
        }
        if (session != null) {
            usedSessions.add(session);
            session.addUsed(this);
        }
        return session;
    }

    /**
     * Make a new session, and return the id
     */
    public WinstoneSession makeNewSession() {
        final String cookieValue = "Winstone_" + remoteIP + "_" + serverPort + "_" + System.currentTimeMillis() + WinstoneRequest.rnd.nextLong();
        final byte digestBytes[] = md5Digester.digest(cookieValue.getBytes());

        // Write out in hex format
        final char outArray[] = new char[32];
        for (int n = 0; n < digestBytes.length; n++) {
            final int hiNibble = (digestBytes[n] & 0xFF) >> 4;
            final int loNibble = (digestBytes[n] & 0xF);
            outArray[2 * n] = (hiNibble > 9 ? (char) (hiNibble + 87) : (char) (hiNibble + 48));
            outArray[(2 * n) + 1] = (loNibble > 9 ? (char) (loNibble + 87) : (char) (loNibble + 48));
        }

        final String newSessionId = new String(outArray);
        currentSessionIds.put(webappConfig.getContextPath(), newSessionId);
        return webappConfig.makeNewSession(newSessionId);
    }

    public void markSessionsAsRequestFinished(final long lastAccessedTime, final boolean saveSessions) {
        for (final Iterator<WinstoneSession> i = usedSessions.iterator(); i.hasNext(); ) {
            final WinstoneSession session = i.next();
            session.setLastAccessedDate(lastAccessedTime);
            session.removeUsed(this);
            if (saveSessions) {
                session.saveToTemp();
            }
        }
        usedSessions.clear();
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String getRealPath(final String path) {
        return webappConfig.getRealPath(path);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    public HttpDecodePart getDecodePart() {
        return decodePart;
    }

    public void setDecodePart(HttpDecodePart decodePart) {
        this.decodePart = decodePart;
    }
}
