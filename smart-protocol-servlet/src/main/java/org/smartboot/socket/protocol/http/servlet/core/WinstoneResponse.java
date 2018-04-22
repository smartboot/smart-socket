/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.http11.Http11Request;
import org.smartboot.socket.protocol.http.WinstoneResourceBundle;
import org.smartboot.socket.protocol.http.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * Response for servlet TODO optimize logging call and load of shared string
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResponse.java,v 1.28 2005/04/19 07:33:41 rickknowles
 * Exp $
 */
public class WinstoneResponse implements HttpServletResponse {

    public static final transient String X_POWERED_BY_HEADER_VALUE = WinstoneResourceBundle.getInstance().getString("PoweredByHeader");
    private static final DateFormat HTTP_DF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static final DateFormat VERSION0_DF = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.US);
    private static final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";
    protected static Logger logger = LogManager.getLogger(WinstoneResponse.class);

    static {
        WinstoneResponse.HTTP_DF.setTimeZone(TimeZone.getTimeZone("GMT"));
        WinstoneResponse.VERSION0_DF.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final List<String> headers;
    private final List<Cookie> cookies;
    /**
     * Of {@link #cookies}, ones that should be marked as "HttpOnly". See
     * http://en.wikipedia.org/wiki/HTTP_cookie#HttpOnly_cookie
     */
    private final Set<Cookie> httpOnlyCookies;
    private int statusCode;
    private WinstoneRequest req;
    private WebAppConfiguration webAppConfig;
    private WinstoneOutputStream outputStream;
    private PrintWriter outputWriter;
    private String explicitEncoding;
    private String implicitEncoding;
    private Locale locale;
    //    private String protocol;
    private String reqKeepAliveHeader;
    private Integer errorStatusCode;

    /**
     * 只能把信息写到 response 对象的 ServletOutputStream 或 Writer 中，
     * 并提交最后写保留在 response 缓冲区 中的内容，或通过显式地调用 ServletResponse 接口的 flushBuffer 方法。
     * 它不能设置响应头信息或调用任何影响响应头信息的方法，
     * HttpServletRequest.getSession()和 HttpServletRequest.getSession(boolean)方法除外。
     * 任何试图设置头信息必须被忽略，如果响应已经提交，
     * 任何调用 HttpServletRequest.getSession()和 HttpServletRequest.getSession(boolean)方法将需要添加一个 Cookie 响应头信息，
     * 且必须抛出一个 IllegalStateException 异常
     */
    private boolean isInclude = false;
    private HttpResponse response;
    private Http11Request request;

    /**
     * Build a new instance of WinstoneResponse.
     */
    public WinstoneResponse(Http11Request request, HttpResponse response) {
        super();
        headers = new ArrayList<String>();
        cookies = new ArrayList<Cookie>();
        httpOnlyCookies = new HashSet<Cookie>();

        statusCode = HttpServletResponse.SC_OK;
        locale = null; // Locale.getDefault();
        explicitEncoding = null;
        reqKeepAliveHeader = null;
        this.response = response;
        this.request = request;
    }

    protected static String getCharsetFromContentTypeHeader(final String type, final StringBuilder remainder) {
        if (type == null) {
            return null;
        }
        // Parse type to set encoding if needed
        final StringTokenizer st = new StringTokenizer(type, ";");
        String localEncoding = null;
        while (st.hasMoreTokens()) {
            final String clause = st.nextToken().trim();
            if (clause.startsWith("charset=")) {
                localEncoding = clause.substring(8);
            } else {
                if (remainder.length() > 0) {
                    remainder.append(";");
                }
                remainder.append(clause);
            }
        }
        if (localEncoding == null || !localEncoding.startsWith("\"") || !localEncoding.endsWith("\"")) {
            return localEncoding;
        } else {
            return localEncoding.substring(1, localEncoding.length() - 1);
        }
    }

    private static String formatHeaderDate(final Date dateIn) {
        String date = null;
        synchronized (WinstoneResponse.HTTP_DF) {
            date = WinstoneResponse.HTTP_DF.format(dateIn);
        }
        return date;
    }

    /**
     * Quotes the necessary strings in a cookie header. The quoting is only
     * applied if the string contains special characters.
     */
    protected static void quote(final String value, final StringBuffer out) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            out.append(value);
        } else {
            boolean containsSpecial = Boolean.FALSE;
            for (int n = 0; n < value.length(); n++) {
                final char thisChar = value.charAt(n);
                if (thisChar < 32 || thisChar >= 127 || WinstoneResponse.specialCharacters.indexOf(thisChar) != -1) {
                    containsSpecial = Boolean.TRUE;
                    break;
                }
            }
            if (containsSpecial) {
                out.append('"').append(value).append('"');
            } else {
                out.append(value);
            }
        }
    }

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        req = null;
        webAppConfig = null;
        outputStream = null;
        outputWriter = null;
        headers.clear();
        cookies.clear();
        httpOnlyCookies.clear();
        reqKeepAliveHeader = null;

        statusCode = HttpServletResponse.SC_OK;
        errorStatusCode = null;
        locale = null; // Locale.getDefault();
        explicitEncoding = null;
        implicitEncoding = null;
    }

    /**
     * Determine witch encoding to use for specified locale.
     *
     * @param locale
     * @return
     */
    private String getEncodingFromLocale(final Locale locale) {
        String localeString = locale.getLanguage() + "_" + locale.getCountry();
        final Map<String, String> encMap = webAppConfig.getLocaleEncodingMap();
        WinstoneResponse.logger.debug("Scanning for locale-encoding match: {} in {}", localeString, encMap + "");

        final String fullMatch = encMap.get(localeString);
        if (fullMatch != null) {
            WinstoneResponse.logger.debug("Found locale-encoding match: {}", fullMatch);
            return fullMatch;
        } else {
            localeString = locale.getLanguage();
            WinstoneResponse.logger.debug("Scanning for locale-encoding match: {} in {}", localeString, encMap + "");
            final String match = encMap.get(localeString);
            if (match != null) {
                WinstoneResponse.logger.debug("Found locale-encoding match: {}", match);
            }
            return match;
        }
    }

    public WinstoneOutputStream getWinstoneOutputStream() {
        return outputStream;
    }

    public void setWebAppConfig(final WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
    }

    public String getProtocol() {
        return request.getHttpVersion();
    }


    public void extractRequestKeepAliveHeader(final WinstoneRequest req) {
        reqKeepAliveHeader = req.getHeader(WinstoneConstant.KEEP_ALIVE_HEADER);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public WinstoneRequest getRequest() {
        return req;
    }

    public void setRequest(final WinstoneRequest req) {
        this.req = req;
    }


    public void setInclude(boolean include) {
        isInclude = include;
    }

    public void finishIncludeBuffer() throws IOException {
        if (outputWriter != null) {
            outputWriter.flush();
        }
    }

    /**
     * This ensures the bare minimum correct http headers are present
     */
    public void validateHeaders() {
        // Need this block for WebDAV support. "Connection:close" header is
        // ignored
        String lengthHeader = getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
        if (lengthHeader == null && statusCode >= 300) {
            final long bodyBytes = outputStream.getOutputStreamLength();
            if (getBufferSize() > bodyBytes) {
                WinstoneResponse.logger.debug("Keep-alive requested but no content length set. Setting to {} bytes", "" + bodyBytes);
                forceHeader(WinstoneConstant.CONTENT_LENGTH_HEADER, "" + bodyBytes);
                lengthHeader = getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
            }
        }

        forceHeader(WinstoneConstant.KEEP_ALIVE_HEADER, !closeAfterRequest() ? WinstoneConstant.KEEP_ALIVE_OPEN : WinstoneConstant.KEEP_ALIVE_CLOSE);
        final String contentType = getHeader(WinstoneConstant.CONTENT_TYPE_HEADER);
        if (statusCode != HttpServletResponse.SC_MOVED_TEMPORARILY) {
            if (contentType == null) {
                // Bypass normal encoding
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, "text/html;charset=" + getCharacterEncoding());
            } else if (contentType.startsWith("text/")) {
                // replace charset in content
                final StringBuilder remainder = new StringBuilder();
                WinstoneResponse.getCharsetFromContentTypeHeader(contentType, remainder);
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, remainder.toString() + ";charset=" + getCharacterEncoding());
            }
        }
        if (getHeader(WinstoneConstant.DATE_HEADER) == null) {
            forceHeader(WinstoneConstant.DATE_HEADER, WinstoneResponse.formatHeaderDate(new Date()));
        }
        if (getHeader(WinstoneConstant.X_POWERED_BY_HEADER) == null) {
            forceHeader(WinstoneConstant.X_POWERED_BY_HEADER, WinstoneResponse.X_POWERED_BY_HEADER_VALUE);
        }
        if (locale != null) {
            String lang = locale.getLanguage();
            if (locale.getCountry() != null && !locale.getCountry().equals("")) {
                lang = lang + "-" + locale.getCountry();
            }
            forceHeader(WinstoneConstant.CONTENT_LANGUAGE_HEADER, lang);
        }

        // If we don't have a webappConfig, exit here, cause we definitely don't
        // have a session
        if (req.getWebAppConfig() == null) {
            return;
        }
        // Write out the new session cookie if it's present
        final HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
        for (final Iterator<String> i = req.getCurrentSessionIds().keySet().iterator(); i.hasNext(); ) {
            final String prefix = i.next();
            final String sessionId = req.getCurrentSessionIds().get(prefix);
            final WebAppConfiguration ownerContext = hostConfig.getWebAppByURI(prefix);
            if (ownerContext != null) {
                final WinstoneSession session = ownerContext.getSessionById(sessionId, Boolean.TRUE);
                if (session != null && session.isNew()) {
                    session.setIsNew(Boolean.FALSE);
                    final Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, session.getId());
                    cookie.setMaxAge(-1);
                    cookie.setSecure(req.isSecure());
                    cookie.setVersion(0); // req.isSecure() ? 1 : 0);
                    cookie.setPath(req.getWebAppConfig().getContextPath().equals("") ? "/" : req.getWebAppConfig().getContextPath());
                    cookies.add(cookie); // don't call addCookie because we
                    // might be including
                    httpOnlyCookies.add(cookie);

                }
            }
        }

        // Look for expired sessions: ie ones where the requested and current
        // ids are different
        for (final Iterator<String> i = req.getRequestedSessionIds().keySet().iterator(); i.hasNext(); ) {
            final String prefix = i.next();
            final String sessionId = req.getRequestedSessionIds().get(prefix);
            if (!req.getCurrentSessionIds().containsKey(prefix)) {
                final Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, sessionId);
                cookie.setMaxAge(0); // explicitly expire this cookie
                cookie.setSecure(req.isSecure());
                cookie.setVersion(0); // req.isSecure() ? 1 : 0);
                cookie.setPath(prefix.equals("") ? "/" : prefix);
                cookies.add(cookie); // don't call addCookie because we might be
                // including
                httpOnlyCookies.add(cookie);
            }
        }
        WinstoneResponse.logger.debug("Headers prepared for writing: {}", "" + headers + "");
    }

    /**
     * Writes out the http header for a single cookie
     */
    public String writeCookie(final Cookie cookie) throws IOException {
        WinstoneResponse.logger.debug("Writing cookie to output: {}", "" + cookie + "");
        final StringBuffer out = new StringBuffer();

        // Set-Cookie or Set-Cookie2
        if (cookie.getVersion() >= 1) {
            out.append(WinstoneConstant.OUT_COOKIE_HEADER1).append(": "); // TCK
            // doesn't
            // like
            // set-cookie2
        } else {
            out.append(WinstoneConstant.OUT_COOKIE_HEADER1).append(": ");
        }

        // name/value pair
        if (cookie.getVersion() == 0) {
            out.append(cookie.getName()).append("=").append(cookie.getValue());
        } else {
            out.append(cookie.getName()).append("=");
            WinstoneResponse.quote(cookie.getValue(), out);
        }

        if (cookie.getVersion() >= 1) {
            out.append("; Version=1");
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                WinstoneResponse.quote(cookie.getDomain(), out);
            }
            if (cookie.getSecure()) {
                out.append("; Secure");
            }

            if (cookie.getMaxAge() >= 0) {
                out.append("; Max-Age=").append(cookie.getMaxAge());
            } else {
                out.append("; Discard");
            }
            if (cookie.getPath() != null) {
                out.append("; Path=");
                WinstoneResponse.quote(cookie.getPath(), out);
            }
        } else {
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                out.append(cookie.getDomain());
            }
            if (cookie.getMaxAge() > 0) {
                final long expiryMS = System.currentTimeMillis() + 1000 * (long) cookie.getMaxAge();
                String expiryDate = null;
                synchronized (WinstoneResponse.VERSION0_DF) {
                    expiryDate = WinstoneResponse.VERSION0_DF.format(new Date(expiryMS));
                }
                out.append("; Expires=").append(expiryDate);
            } else if (cookie.getMaxAge() == 0) {
                String expiryDate = null;
                synchronized (WinstoneResponse.VERSION0_DF) {
                    expiryDate = WinstoneResponse.VERSION0_DF.format(new Date(5000));
                }
                out.append("; Expires=").append(expiryDate);
            }
            if (cookie.getPath() != null) {
                out.append("; Path=").append(cookie.getPath());
            }
            if (cookie.getSecure()) {
                out.append("; Secure");
            }
        }
        if (httpOnlyCookies.contains(cookie)) {
            out.append("; HttpOnly");
        }
        return out.toString();
    }

    /**
     * Based on request/response headers and the protocol, determine whether or
     * not this connection should operate in keep-alive mode.
     */
    public boolean closeAfterRequest() {
        final String inKeepAliveHeader = reqKeepAliveHeader;
        final String outKeepAliveHeader = getHeader(WinstoneConstant.KEEP_ALIVE_HEADER);
        final boolean hasContentLength = getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER) != null;
        if (request.getHttpVersion().startsWith("HTTP/0")) {
            return Boolean.TRUE;
        } else if (inKeepAliveHeader == null && outKeepAliveHeader == null) {
            return request.getHttpVersion().equals("HTTP/1.0") ? Boolean.TRUE : !hasContentLength;
        } else if (outKeepAliveHeader != null) {
            return outKeepAliveHeader.equalsIgnoreCase(WinstoneConstant.KEEP_ALIVE_CLOSE) || !hasContentLength;
        } else if (inKeepAliveHeader != null) {
            return inKeepAliveHeader.equalsIgnoreCase(WinstoneConstant.KEEP_ALIVE_CLOSE) || !hasContentLength;
        } else {
            return Boolean.FALSE;
        }
    }

    // ServletResponse interface methods
    @Override
    public void flushBuffer() throws IOException {
        if (outputWriter != null) {
            outputWriter.flush();
        }
        try {
            outputStream.flush();
        } catch (final ClientSocketException e) {
            // ignore this error as it's not interesting enough to log
        }
    }

    @Override
    public int getBufferSize() {
        return (int) outputStream.getBufferSize();
    }

    @Override
    public void setBufferSize(final int size) {
        outputStream.setBufferSize(size);
    }

    @Override
    public String getCharacterEncoding() {
        final String enc = getCurrentEncoding();
        return enc == null ? "ISO-8859-1" : enc;
    }

    @Override
    public void setCharacterEncoding(final String encoding) {
        if (outputWriter == null && !isCommitted()) {
            WinstoneResponse.logger.debug("Setting response character encoding to {}", encoding);
            explicitEncoding = encoding;
            correctContentTypeHeaderEncoding(encoding);
        }
    }

    private void correctContentTypeHeaderEncoding(final String encoding) {
        final String contentType = getContentType();
        if (contentType != null) {
            final StringBuilder remainderHeader = new StringBuilder();
            WinstoneResponse.getCharsetFromContentTypeHeader(contentType, remainderHeader);
            if (remainderHeader.length() != 0) {
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, remainderHeader + ";charset=" + encoding);
            }
        }
    }

    @Override
    public String getContentType() {
        return getHeader(WinstoneConstant.CONTENT_TYPE_HEADER);
    }

    @Override
    public void setContentType(final String type) {
        setHeader(WinstoneConstant.CONTENT_TYPE_HEADER, type);
    }

    @Override
    public Locale getLocale() {
        return locale == null ? Locale.getDefault() : locale;
    }

    @Override
    public void setLocale(final Locale loc) {
        if (isInclude) {
            return;
        } else if (isCommitted()) {
            WinstoneResponse.logger.warn("Response.setLocale() ignored, because getWriter already called");
        } else {
            if (outputWriter == null && explicitEncoding == null) {
                final String localeEncoding = getEncodingFromLocale(loc);
                if (localeEncoding != null) {
                    implicitEncoding = localeEncoding;
                    correctContentTypeHeaderEncoding(localeEncoding);
                }
            }
            locale = loc;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        WinstoneResponse.logger.debug("Called ServletResponse.getOutputStream()");
        return outputStream;
    }

    public void setOutputStream(final WinstoneOutputStream outData) {
        outputStream = outData;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        WinstoneResponse.logger.debug("Called ServletResponse.getWriter()");
        if (outputWriter != null) {
            return outputWriter;
        } else {
            outputWriter = new WinstoneResponseWriter(outputStream, this);
            return outputWriter;
        }
    }

    @Override
    public boolean isCommitted() {
        return outputStream.isCommitted();
    }

    @Override
    public void reset() {
        if (!isInclude) {
            resetBuffer();
            statusCode = HttpServletResponse.SC_OK;
            headers.clear();
            cookies.clear();
            httpOnlyCookies.clear();
        }
    }

    @Override
    public void resetBuffer() {
        if (!isInclude) {
            if (isCommitted()) {
                throw new IllegalStateException("Response cannot be reset - it is already committed");
            }

            // Disregard any output temporarily while we flush
            outputStream.setDisregardMode(Boolean.TRUE);

            if (outputWriter != null) {
                outputWriter.flush();
            }

            outputStream.setDisregardMode(Boolean.FALSE);
            outputStream.reset();
        }
    }

    public void setContentLength(final long len) {
        setHeader(WinstoneConstant.CONTENT_LENGTH_HEADER, Long.toString(len));
    }

    @Override
    public void setContentLength(final int len) {
        setIntHeader(WinstoneConstant.CONTENT_LENGTH_HEADER, len);
    }

    // HttpServletResponse interface methods
    @Override
    public void addCookie(final Cookie cookie) {
        if (!isInclude) {
            cookies.add(cookie);
        }
    }

    @Override
    public boolean containsHeader(final String name) {
        for (String header : this.headers) {
            if (header.startsWith(name)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public void addDateHeader(final String name, final long date) {
        addHeader(name, WinstoneResponse.formatHeaderDate(new Date(date)));
    } // df.format(new Date(date)));}

    @Override
    public void addIntHeader(final String name, final int value) {
        addHeader(name, "" + value);
    }

    @Override
    public void addHeader(final String name, String value) {
        if (isInclude) {
            WinstoneResponse.logger.debug("Header ignored inside include - {}: {} ", name, value);
        } else if (isCommitted()) {
            WinstoneResponse.logger.debug("Header ignored after response committed - {}: {} ", name, value);
        } else if (value != null) {
            if (name.equals(WinstoneConstant.CONTENT_TYPE_HEADER)) {
                final StringBuilder remainderHeader = new StringBuilder();
                final String headerEncoding = WinstoneResponse.getCharsetFromContentTypeHeader(value, remainderHeader);
                if (outputWriter != null) {
                    value = remainderHeader + ";charset=" + getCharacterEncoding();
                } else if (headerEncoding != null) {
                    explicitEncoding = headerEncoding;
                }
            }
            headers.add(name + ": " + value);
        }
    }

    @Override
    public void setDateHeader(final String name, final long date) {
        setHeader(name, WinstoneResponse.formatHeaderDate(new Date(date)));
    }

    @Override
    public void setIntHeader(final String name, final int value) {
        setHeader(name, "" + value);
    }

    @Override
    public void setHeader(final String name, String value) {
        if (isInclude) {
            WinstoneResponse.logger.debug("Header ignored inside include - {}: {} ", name, value);
        } else if (isCommitted()) {
            WinstoneResponse.logger.debug("Header ignored after response committed - {}: {} ", name, value);
        } else {
            boolean found = Boolean.FALSE;
            for (int n = 0; n < headers.size(); n++) {
                final String header = headers.get(n);
                if (header.startsWith(name + ": ")) {
                    if (found) {
                        headers.remove(n);
                        continue;
                    }
                    if (name.equals(WinstoneConstant.CONTENT_TYPE_HEADER)) {
                        if (value != null) {
                            final StringBuilder remainderHeader = new StringBuilder();
                            final String headerEncoding = WinstoneResponse.getCharsetFromContentTypeHeader(value, remainderHeader);
                            if (outputWriter != null) {
                                value = remainderHeader + ";charset=" + getCharacterEncoding();
                            } else if (headerEncoding != null) {
                                explicitEncoding = headerEncoding;
                            }
                        }
                    }

                    if (value != null) {
                        headers.set(n, name + ": " + value);
                    } else {
                        headers.remove(n);
                    }
                    found = Boolean.TRUE;
                }
            }
            if (!found) {
                addHeader(name, value);
            }
        }
    }

    private void forceHeader(final String name, final String value) {
        boolean found = Boolean.FALSE;
        for (int n = 0; n < headers.size(); n++) {
            final String header = headers.get(n);
            if (header.startsWith(name + ": ")) {
                found = Boolean.TRUE;
                headers.set(n, name + ": " + value);
            }
        }
        if (!found) {
            headers.add(name + ": " + value);
        }
    }

    private String getCurrentEncoding() {
        if (explicitEncoding != null) {
            return explicitEncoding;
        } else if (implicitEncoding != null) {
            return implicitEncoding;
        } else if (req != null && req.getCharacterEncoding() != null) {
            try {
                "0".getBytes(req.getCharacterEncoding());
                return req.getCharacterEncoding();
            } catch (final UnsupportedEncodingException err) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getHeader(final String name) {
        for (int n = 0; n < headers.size(); n++) {
            final String header = headers.get(n);
            if (header.startsWith(name + ": ")) {
                return header.substring(name.length() + 2);
            }
        }
        return null;
    }

    @Override
    public String encodeRedirectURL(final String url) {
        return url;
    }

    @Override
    public String encodeURL(final String url) {
        return url;
    }

    public int getStatus() {
        return statusCode;
    }

    @Override
    public void setStatus(final int sc) {
        if (!isInclude && errorStatusCode == null) {
            // if (!isIncluding()) {
            statusCode = sc;
            // if (this.errorStatusCode != null) {
            // this.errorStatusCode = new Integer(sc);
            // }
        }
    }

    public Integer getErrorStatusCode() {
        return errorStatusCode;
    }

    public void setErrorStatusCode(final int statusCode) {
        errorStatusCode = new Integer(statusCode);
        this.statusCode = statusCode;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isInclude) {
            WinstoneResponse.logger.error("Ignoring redirect in include: " + location);
            return;
        } else if (isCommitted()) {
            throw new IllegalStateException("OutputStream already committed");
        }
        resetBuffer();

        // Build location
        final StringBuilder fullLocation = new StringBuilder();
        if (location.startsWith("http://") || location.startsWith("https://")) {
            fullLocation.append(location);
        } else {
            if (location.trim().equals(".")) {
                location = "";
            }

            fullLocation.append(req.getScheme()).append("://");
            fullLocation.append(req.getServerName());
            if (!(req.getServerPort() == 80 && req.getScheme().equals("http")) && !(req.getServerPort() == 443 && req.getScheme().equals("https"))) {
                fullLocation.append(':').append(req.getServerPort());
            }
            if (location.startsWith("/")) {
                fullLocation.append(location);
            } else {
                fullLocation.append(req.getRequestURI());
                final int questionPos = fullLocation.toString().indexOf("?");
                if (questionPos != -1) {
                    fullLocation.delete(questionPos, fullLocation.length());
                }
                fullLocation.delete(fullLocation.toString().lastIndexOf("/") + 1, fullLocation.length());
                fullLocation.append(location);
            }
        }
        if (req != null) {
            req.discardRequestBody();
        }
        statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        setHeader(WinstoneConstant.LOCATION_HEADER, fullLocation.toString());
        setContentLength(0);
        getWriter().flush();
    }

    @Override
    public void sendError(final int sc) throws IOException {
        sendError(sc, null);
    }

    @Override
    public void sendError(final int sc, final String msg) throws IOException {
        if (isInclude) {
            WinstoneResponse.logger.error("Error in include: {} {}", "" + sc, msg);
            return;
        }
        WinstoneResponse.logger.debug("Sending error message to browser: code {}, message: {}", "" + sc, msg);
        if (webAppConfig != null && req != null) {

            final SimpleRequestDispatcher rd = webAppConfig.getErrorDispatcherByCode(req.getRequestURI(), sc, msg, null);
            if (rd != null) {
                try {
                    rd.forward(req, this);
                    return;
                } catch (final IllegalStateException err) {
                    throw err;
                } catch (final IOException err) {
                    throw err;
                } catch (final Throwable err) {
                    if (WinstoneResponse.logger.isWarnEnabled()) {
                        WinstoneResponse.logger.warn("Sending error message to browser: code " + rd.getName() + ", message: " + sc, err);
                    }
                    return;
                }
            }
        }
        // If we are here there was no webapp and/or no request object, so
        // show the default error page
        if (errorStatusCode == null) {
            statusCode = sc;
        }
        final String output = WinstoneResourceBundle.getInstance().getString("WinstoneResponse.ErrorPage", sc + "", msg == null ? "" : StringUtils.htmlEscapeBasicMarkup(msg), "", WinstoneResourceBundle.getInstance().getString("ServerVersion"),
                "" + new Date());
        setContentLength(output.getBytes(getCharacterEncoding()).length);
        final Writer out = getWriter();
        out.write(output);
        out.flush();
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String encodeRedirectUrl(final String url) {
        return encodeRedirectURL(url);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String encodeUrl(final String url) {
        return encodeURL(url);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void setStatus(final int sc, final String sm) {
        setStatus(sc);
    }
}
