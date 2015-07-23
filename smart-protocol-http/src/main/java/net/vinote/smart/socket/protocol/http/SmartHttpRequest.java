package net.vinote.smart.socket.protocol.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import net.vinote.smart.socket.lang.SmartByteBuffer;
import net.vinote.smart.socket.lang.SmartEnumeration;
import net.vinote.smart.socket.protocol.DataEntry;

public class SmartHttpRequest extends DataEntry {
	private String method;

	private String requestURI;

	private String protocol;

	private int remotePort;

	private String remoteHost;

	private String remoteAddr;

	private String contextPath;

	// private String servletPath = "";

	private String queryString;

	private Map<String, List<String>> headMap = new HashMap<String, List<String>>();

	private Map<String, String[]> parameterMap = new HashMap<String, String[]>();

	private Map<String, Object> attributeMap = new HashMap<String, Object>();

	private boolean paramParsed = false;

	private SmartByteBuffer formBuf = new SmartByteBuffer();

	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Enumeration getAttributeNames() {
		return new SmartEnumeration(attributeMap.keySet().iterator());
	}

	public String getCharacterEncoding() {
		return null;
	}

	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {

	}

	public int getContentLength() {

		return 0;
	}

	public String getContentType() {
		return getHeader("Content-Type");
	}

	public String getParameter(String name) {
		parseParameters();
		String[] valus = parameterMap.get(name);
		return valus != null && valus.length >= 0 ? valus[0] : null;
	}

	public Enumeration<String> getParameterNames() {
		parseParameters();
		return new SmartEnumeration<String>(parameterMap.keySet().iterator());
	}

	public String[] getParameterValues(String name) {
		parseParameters();
		return parameterMap.get(name);
	}

	public Map<String, String[]> getParameterMap() {
		parseParameters();
		return parameterMap;
	}

	private void parseParameters() {
		if (!paramParsed) {
			formBuf.lock();
			StringBuffer sb = new StringBuffer();
			if (getQueryString() != null) {
				sb.append(getQueryString()).append("&");

			}
			sb.append(formBuf.toString());
			StringTokenizer st = new StringTokenizer(sb.toString(), "&");
			Map<String, List<String>> paramMap = new HashMap<String, List<String>>();
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				int pos = pair.indexOf('=');
				if (pos == -1) {
					continue;
				}
				String key = pair.substring(0, pos);
				String val = pair.substring(pos + 1, pair.length());
				List<String> valueList = paramMap.get(key);
				if (valueList == null) {
					valueList = new ArrayList<String>();
					paramMap.put(key, valueList);
				}
				valueList.add(val);
			}
			for (Entry<String, List<String>> entry : paramMap.entrySet()) {
				String[] params = new String[entry.getValue().size()];
				parameterMap.put(entry.getKey(),
						entry.getValue().toArray(params));
			}
			paramParsed = true;
		}
	}

	public String getProtocol() {
		return protocol;
	}

	public String getScheme() {

		return null;
	}

	public String getServerName() {
		return null;
	}

	public int getServerPort() {
		return 0;
	}

	public BufferedReader getReader() throws IOException {

		return null;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setAttribute(String name, Object o) {

	}

	public void removeAttribute(String name) {

	}

	public Locale getLocale() {

		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	public Enumeration getLocales() {

		return null;
	}

	public boolean isSecure() {

		return false;
	}

	public String getRealPath(String path) {

		return null;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public String getLocalName() {

		return null;
	}

	public String getLocalAddr() {

		return null;
	}

	public int getLocalPort() {

		return 0;
	}

	public String getAuthType() {

		return null;
	}

	public long getDateHeader(String name) {

		return 0;
	}

	public String getHeader(String name) {
		List<String> list = headMap.get(name);
		return list != null && list.size() > 0 ? list.get(0) : null;
	}

	public Enumeration<String> getHeaders(String name) {
		return new SmartEnumeration<String>(headMap.get(name).iterator());
	}

	public Enumeration<String> getHeaderNames() {
		return new SmartEnumeration<String>(headMap.keySet().iterator());
	}

	public int getIntHeader(String name) {

		return 0;
	}

	public String getMethod() {

		return method;
	}

	// public String getPathInfo() {
	//
	// return null;
	// }

	public String getPathTranslated() {

		return null;
	}

	public String getContextPath() {
		return contextPath;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getRemoteUser() {

		return null;
	}

	public boolean isUserInRole(String role) {

		return false;
	}

	public Principal getUserPrincipal() {

		return null;
	}

	public String getRequestedSessionId() {

		return null;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public StringBuffer getRequestURL() {

		return null;
	}

	// public String getServletPath() {
	// return servletPath;
	// }

	public boolean isRequestedSessionIdValid() {

		return false;
	}

	public boolean isRequestedSessionIdFromCookie() {

		return false;
	}

	public boolean isRequestedSessionIdFromURL() {

		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {

		return false;
	}

	// /////////////////////////////////////////////////////////////

	public final void setMethod(String method) {
		this.method = method;
	}

	public final void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public final void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public final void addHeader(String name, String value) {
		List<String> list = headMap.get(name);
		if (list == null) {
			list = new ArrayList<String>();
			headMap.put(name, list);
		}
		if (!list.contains(value)) {
			list.add(value);
		}
	}

	public final void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public final SmartByteBuffer getPostFormBuffer() {
		return formBuf;
	}

	public byte[] encode() {
		throw new UnsupportedOperationException();
	}

	public void decode() {
		throw new UnsupportedOperationException();
	}

	public final boolean hasParsedParameters() {
		return paramParsed;
	}

	public final void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public final void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * @param remoteAddr
	 *            the remoteAddr to set
	 */
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public final void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void parse() {
		if (!requestURI.startsWith("/")) {
			throw new IllegalStateException("invalid request URI :"
					+ requestURI);
		}
		String[] arrays = requestURI.split("/");
		contextPath = "/";
		if (arrays.length > 2) {
			contextPath = contextPath + arrays[1];
		}
		// StringTokenizer stoken = new StringTokenizer(requestURI, "/");
		// while (stoken.hasMoreTokens()) {
		// if (contextPath == null) {
		// contextPath = "/" + stoken.nextToken();
		// } else {
		// servletPath += "/" + stoken.nextToken();
		// }
		// }

	}
}
