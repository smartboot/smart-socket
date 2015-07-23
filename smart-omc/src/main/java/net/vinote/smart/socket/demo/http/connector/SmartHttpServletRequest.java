package net.vinote.smart.socket.demo.http.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.vinote.smart.socket.demo.http.server.SmartHttpSession;
import net.vinote.smart.socket.protocol.http.SmartHttpRequest;

public class SmartHttpServletRequest implements HttpServletRequest {
	private SmartHttpRequest request;
	private ServletContext context;
	private String pathInfo;
	private String servletPath;

	public SmartHttpServletRequest(SmartHttpRequest request,
			ServletContext context) {
		this.request = request;
		this.context = context;
	}

	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		throw new UnsupportedOperationException();
	}

	public String getCharacterEncoding() {
		return request.getCharacterEncoding() == null ? "utf-8" : request
				.getCharacterEncoding();
	}

	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {

	}

	public int getContentLength() {

		return 0;
	}

	public String getContentType() {

		throw new UnsupportedOperationException();
	}

	public ServletInputStream getInputStream() throws IOException {

		throw new UnsupportedOperationException();
	}

	public String getParameter(String name) {
		return request.getParameter(name);
	}

	public Enumeration<String> getParameterNames() {
		return request.getParameterNames();
	}

	public String[] getParameterValues(String name) {
		return request.getParameterValues(name);
	}

	public Map<String, String[]> getParameterMap() {
		return request.getParameterMap();
	}

	public String getProtocol() {

		throw new UnsupportedOperationException();
	}

	public String getScheme() {

		throw new UnsupportedOperationException();
	}

	public String getServerName() {

		throw new UnsupportedOperationException();
	}

	public int getServerPort() {

		return 0;
	}

	public BufferedReader getReader() throws IOException {

		throw new UnsupportedOperationException();
	}

	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	public String getRemoteHost() {
		return request.getRemoteHost();
	}

	public void setAttribute(String name, Object o) {

	}

	public void removeAttribute(String name) {

	}

	public Locale getLocale() {
		return request.getLocale() == null ? Locale.getDefault() : request
				.getLocale();
	}

	public Enumeration<Locale> getLocales() {

		throw new UnsupportedOperationException();
	}

	public boolean isSecure() {

		return false;
	}

	public RequestDispatcher getRequestDispatcher(String path) {

		if (context == null)
			return null;

		if (path == null) {
			return null;
		} else if (path.startsWith("/")) {
			return context.getRequestDispatcher(path);
		}

		String servletPath = (String) getAttribute("javax.servlet.include.servlet_path");
		if (servletPath == null)
			servletPath = getServletPath();

		String pathInfo = getPathInfo();
		String requestPath = null;

		if (pathInfo == null) {
			requestPath = servletPath;
		} else {
			requestPath = servletPath + pathInfo;
		}

		int pos = requestPath.lastIndexOf('/');
		String relative = null;
		if (pos >= 0) {
			relative = requestPath.substring(0, pos + 1) + path;
		} else {
			relative = requestPath + path;
		}

		return context.getRequestDispatcher(relative);

	}

	public String getRealPath(String path) {

		throw new UnsupportedOperationException();
	}

	public int getRemotePort() {
		return request.getRemotePort();
	}

	public String getLocalName() {

		throw new UnsupportedOperationException();
	}

	public String getLocalAddr() {

		throw new UnsupportedOperationException();
	}

	public int getLocalPort() {

		return 0;
	}

	public String getAuthType() {

		throw new UnsupportedOperationException();
	}

	public Cookie[] getCookies() {

		throw new UnsupportedOperationException();
	}

	public long getDateHeader(String name) {

		return 0;
	}

	public String getHeader(String name) {

		return request.getHeader(name);
	}

	public Enumeration<String> getHeaders(String name) {
		return request.getHeaders(name);
	}

	public Enumeration<String> getHeaderNames() {
		return request.getHeaderNames();
	}

	public int getIntHeader(String name) {

		return 0;
	}

	public String getMethod() {

		return request.getMethod();
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getPathTranslated() {

		throw new UnsupportedOperationException();
	}

	public String getContextPath() {
		return request.getContextPath();
	}

	public String getQueryString() {

		return request.getQueryString();
	}

	public String getRemoteUser() {

		throw new UnsupportedOperationException();
	}

	public boolean isUserInRole(String role) {

		return false;
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public String getRequestedSessionId() {
		throw new UnsupportedOperationException();
	}

	public String getRequestURI() {

		return request.getRequestURI();
	}

	public StringBuffer getRequestURL() {

		throw new UnsupportedOperationException();
	}

	public String getServletPath() {
		return servletPath;
	}

	public HttpSession getSession(boolean create) {
		return new SmartHttpSession();
	}

	public HttpSession getSession() {
		return new SmartHttpSession();
	}

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

	public final void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public final void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

}
