/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.smartboot.socket.protocol.http.servlet.core;

/**
 * Internal Server Constant.
 * 
 * @author Jerome Guibert
 */
public class WinstoneConstant {

	// Response header constants
	public static final transient String CONTENT_LENGTH_HEADER = "Content-Length";
	public static final transient String CONTENT_TYPE_HEADER = "Content-Type";
	public static final transient String CONTENT_LANGUAGE_HEADER = "Content-Language";
	public static final transient String KEEP_ALIVE_HEADER = "Connection";
	public static final transient String KEEP_ALIVE_OPEN = "Keep-Alive";
	public static final transient String KEEP_ALIVE_CLOSE = "Close";
	public static final transient String DATE_HEADER = "Date";
	public static final transient String LOCATION_HEADER = "Location";
	public static final transient String OUT_COOKIE_HEADER1 = "Set-Cookie";
	public static final transient String X_POWERED_BY_HEADER = "X-Powered-By";
	public static final transient String AUTHORIZATION_HEADER = "Authorization";
	public static final transient String LOCALE_HEADER = "Accept-Language";
	public static final transient String HOST_HEADER = "Host";
	public static final transient String IN_COOKIE_HEADER1 = "Cookie";
	public static final transient String IN_COOKIE_HEADER2 = "Cookie2";
	public static final transient String METHOD_HEAD = "HEAD";
	public static final transient String METHOD_GET = "GET";
	public static final transient String METHOD_POST = "POST";
	public static final transient String POST_PARAMETERS = "application/x-www-form-urlencoded";
	// Include constants
	public static final transient String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";
	public static final transient String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
	public static final transient String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
	public static final transient String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
	public static final transient String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";
	// Forward constants
	public static final transient String FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
	public static final transient String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
	public static final transient String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
	public static final transient String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
	public static final transient String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";
	// Error constants
	public static final transient String ERROR_STATUS_CODE = "javax.servlet.error.status_code";
	public static final transient String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
	public static final transient String ERROR_MESSAGE = "javax.servlet.error.message";
	public static final transient String ERROR_EXCEPTION = "javax.servlet.error.exception";
	public static final transient String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
	public static final transient String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
	// JSP Constant
	public static final transient String JSP_SERVLET_NAME = "JspServlet";
	public static final transient String JSP_SERVLET_CLASS = "org.apache.jasper.servlet.JspServlet";
	public static final transient String JAVAX_JSP_FACTORY = "javax.servlet.jsp.JspFactory";
	// default value
	public static final transient Integer DEFAULT_MAXIMUM_PARAMETER_ALLOWED = 10000;
}
