package net.vinote.smart.socket.demo.http.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import net.vinote.smart.socket.demo.http.connector.SmartHttpServletRequest;
import net.vinote.smart.socket.demo.http.connector.SmartHttpServletResponse;
import net.vinote.smart.socket.demo.http.filter.SmartFilterChain;
import net.vinote.smart.socket.demo.http.filter.SmartFilterModel;
import net.vinote.smart.socket.demo.http.filter.SmartServletModel;
import net.vinote.smart.socket.lang.SmartEnumeration;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.http.RequestUnit;
import net.vinote.smart.socket.protocol.http.SmartHttpRequest;
import net.vinote.smart.socket.transport.TransportSession;

public abstract class AbstractServletContext implements ServletContext {
	private static final RunLogger logger = RunLogger.getLogger();
	/** 应用名称 */
	private String contextName;

	private Map<String, String> parameterMap = new HashMap<String, String>();

	private Map<String, Object> attributeMap = new HashMap<String, Object>();
	private Servlet unfindServlet = new UnfoundServlet();
	protected String basePath;

	protected Map<String, SmartServletModel> servletList = new HashMap<String, SmartServletModel>();
	protected List<String> servletPatternList = new ArrayList<String>();
	protected List<SmartFilterModel> filterModelList = new ArrayList<SmartFilterModel>();

	// Servlet上下文事件
	/**
	 * 生命周期:Servlet上下文已经创建，同时服务第一个请求，或者servlet上下文将要被关闭。
	 */
	protected List<ServletContextListener> servletContextListenerList = new ArrayList<ServletContextListener>();

	/**
	 * 属性的变化:Servlet上下文中属性被添加、移除或替换。 Attributes on the servlet context have been
	 * added, removed, or replaced.
	 */
	protected List<ServletContextAttributeListener> servletContextAttributeListenerList = new ArrayList<ServletContextAttributeListener>();

	// HTTP session事件
	/**
	 * 生命周期:HttpSession被创建，作废或超时。
	 */
	protected List<HttpSessionListener> httpSessionListenerList = new ArrayList<HttpSessionListener>();

	/**
	 * 属性的变化:HttpSession中的属性被添加、移除或修改
	 */
	protected List<HttpSessionAttributeListener> httpSessionAttributeListenerList = new ArrayList<HttpSessionAttributeListener>();

	/**
	 * Session迁移:HttpSession被激活或钝化
	 */
	protected List<HttpSessionActivationListener> httpSessionActivationListenerList = new ArrayList<HttpSessionActivationListener>();

	/**
	 * 对象绑定:对象和Httpsession绑定或从Httpsession中unbound
	 */
	protected List<HttpSessionBindingListener> httpSessionBindingListenerList = new ArrayList<HttpSessionBindingListener>();

	// Servlet请求事件
	/**
	 * 生命周期:Servlet请求由web容器开始执行。 A servlet request has started being processed
	 * by Web components.
	 */
	protected List<ServletRequestListener> servletRequestListenerList = new ArrayList<ServletRequestListener>();

	/**
	 * 属性的变化:ServletRequest中的属性被添加、移除或替换。
	 */
	protected List<ServletRequestAttributeListener> servletRequestAttributeListenerList = new ArrayList<ServletRequestAttributeListener>();

	public void servie(RequestUnit unit) {
		SmartHttpRequest smartRequest = unit.getRequest();
		TransportSession tranSession = unit.getTransport();
		smartRequest.setRemotePort(tranSession.getRemotePort());
		smartRequest.setRemoteHost(tranSession.getRemoteHost());
		smartRequest.setRemoteAddr(tranSession.getRemoteAddr());
		HttpServletRequest request = new SmartHttpServletRequest(smartRequest, this);
		smartRequest.setContextPath("/" + contextName);
		if (!request.getRequestURI().startsWith(request.getContextPath())) {
			throw new IllegalStateException("contextPath解析异常:uri=" + request.getRequestURI() + " ,contextPaht="
				+ request.getContextPath());
		}
		HttpServletResponse response = new SmartHttpServletResponse(tranSession);
		service(request, response, request.getRequestURI().substring(request.getContextPath().length()));

		// logger.log(Level.SEVERE, unit.request.toString());
		try {
			response.getWriter().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String uri = "/hello/aa.col";
		Pattern p = Pattern.compile("^.*(llo).*$");
		Matcher m = p.matcher(uri);
		System.out.println(m.end());
		System.out.println(m.matches());
		System.out.println(m.regionStart());
		System.out.println(m.regionEnd());
	}

	void service(ServletRequest request, ServletResponse response, String uriWithoutContextPaht) {
		if (!uriWithoutContextPaht.startsWith("/")) {
			uriWithoutContextPaht = "/" + uriWithoutContextPaht;
		}
		Servlet servlet = null;
		int maxIndex = -1;
		for (SmartServletModel entry : servletList.values()) {
			// 最大匹配Servlet
			int index = entry.matches(uriWithoutContextPaht);
			if (index > maxIndex) {
				maxIndex = index;
				if (request instanceof SmartHttpServletRequest) {
					SmartHttpServletRequest req = (SmartHttpServletRequest) request;
					req.setServletPath(uriWithoutContextPaht.substring(0, index));
					if (index < (uriWithoutContextPaht.length())) {
						req.setPathInfo(uriWithoutContextPaht.substring(index + 1));
					}
				}
				servlet = entry.getServlet();
			}
		}
		if (servlet == null) {
			servlet = unfindServlet;
		}
		try {
			FilterChain filterChain = getFilterChain(uriWithoutContextPaht, servlet);
			filterChain.doFilter(request, response);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ServletException e1) {
			e1.printStackTrace();
		}
	}

	private FilterChain getFilterChain(String uri, Servlet servlet) {
		List<Filter> filters = new ArrayList<Filter>();
		for (SmartFilterModel model : filterModelList) {
			if (model.matches(uri) > 0) {
				filters.add(model.getFilter());
			}
		}
		FilterChain chain = new SmartFilterChain(servlet, filters);
		return chain;
	}

	public final void init(String basePath) throws Exception {
		if (basePath != null) {
			contextName = new File(basePath).getName();
			this.basePath = basePath;
		}
		init0(basePath);
		ServletContextEvent event = new ServletContextEvent(this);
		for (ServletContextListener listener : servletContextListenerList) {
			listener.contextInitialized(event);
		}
	}

	protected abstract void init0(String serverPath) throws Exception;

	public final void destory() {
		ServletContextEvent event = new ServletContextEvent(this);
		for (ServletContextListener listener : servletContextListenerList) {
			listener.contextDestroyed(event);
		}
	}

	protected abstract void destory0();

	public final String getServerName() {
		return contextName;
	}

	public final void setServerName(String serverName) {
		this.contextName = serverName;
	}

	public Object getAttribute(String arg0) {
		return attributeMap.get(arg0);
	}

	public Enumeration<String> getAttributeNames() {
		return new SmartEnumeration<String>(attributeMap.keySet().iterator());
	}

	public ServletContext getContext(String arg0) {
		throw new UnsupportedOperationException();
	}

	public String getContextPath() {
		return "/" + contextName;
	}

	public String getInitParameter(String arg0) {

		return parameterMap.get(arg0);
	}

	public Enumeration<String> getInitParameterNames() {
		return new SmartEnumeration<String>(parameterMap.keySet().iterator());
	}

	public int getMajorVersion() {
		return 0;
	}

	public String getMimeType(String arg0) {
		throw new UnsupportedOperationException();
	}

	public int getMinorVersion() {
		return 0;
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		throw new UnsupportedOperationException();
	}

	public String getRealPath(String path) {
		return basePath + path;
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		if (path == null) {
			return null;
		}
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("invalid dispatcher " + path);
		}
		// String queryString = null;
		int pos = path.indexOf('?');
		if (pos >= 0) {
			// queryString = path.substring(pos + 1);
			path = path.substring(0, pos);
		}
		if (path == null)
			return (null);

		pos = path.length();

		return new SmartRequestDispatcher(path, this);
	}

	public URL getResource(String arg0) throws MalformedURLException {
		if (!arg0.startsWith("/")) {
			return null;
		}
		URL url = new URL("file:/" + basePath + File.separatorChar + arg0.substring(1));
		return url;
	}

	public InputStream getResourceAsStream(String arg0) {
		try {
			URL url = getResource(arg0);
			return url == null ? null : url.openStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Set<String> getResourcePaths(String arg0) {
		throw new UnsupportedOperationException();
	}

	public String getServerInfo() {
		throw new UnsupportedOperationException();
	}

	public Servlet getServlet(String arg0) throws ServletException {
		SmartServletModel model = servletList.get(arg0);
		return model == null ? null : model.getServlet();
	}

	public String getServletContextName() {
		return contextName;
	}

	public Enumeration<String> getServletNames() {
		return new SmartEnumeration<String>(servletList.keySet().iterator());
	}

	public Enumeration<Servlet> getServlets() {
		List<Servlet> list = new ArrayList<Servlet>(servletList.size());
		for (SmartServletModel model : servletList.values()) {
			list.add(model.getServlet());
		}
		return new SmartEnumeration<Servlet>(list.iterator());
	}

	public void log(String arg0) {

	}

	public void log(Exception arg0, String arg1) {

	}

	public void log(String arg0, Throwable arg1) {

	}

	public void removeAttribute(String arg0) {
		ServletContextAttributeEvent event = new ServletContextAttributeEvent(AbstractServletContext.this, arg0, null);
		for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
			listener.attributeRemoved(event);
		}
		attributeMap.remove(arg0);
	}

	/**
	 * Validate the syntax of a proposed <code>&lt;url-pattern&gt;</code> for
	 * conformance with specification requirements.
	 *
	 * @param urlPattern
	 *            URL pattern to be validated
	 */
	protected boolean validateURLPattern(String urlPattern) {

		if (urlPattern == null)
			return (false);
		if (urlPattern.indexOf('\n') >= 0 || urlPattern.indexOf('\r') >= 0) {
			return (false);
		}
		if (urlPattern.startsWith("*.")) {
			if (urlPattern.indexOf('/') < 0) {
				checkUnusualURLPattern(urlPattern);
				return (true);
			} else
				return (false);
		}
		if ((urlPattern.startsWith("/")) && (urlPattern.indexOf("*.") < 0)) {
			checkUnusualURLPattern(urlPattern);
			return (true);
		} else
			return (false);

	}

	/**
	 * Check for unusual but valid <code>&lt;url-pattern&gt;</code>s. See
	 * Bugzilla 34805, 43079 & 43080
	 */
	private void checkUnusualURLPattern(String urlPattern) {
		if (urlPattern.endsWith("*") && (urlPattern.length() < 2 || urlPattern.charAt(urlPattern.length() - 2) != '/')) {
			logger.log(Level.FINE, "Suspicious url pattern: \"" + urlPattern + "\"" + " in context [" + contextName
				+ "] - see" + " section SRV.11.2 of the Servlet specification");
		}
	}

	public void setAttribute(String key, Object value) {
		ServletContextAttributeEvent event = new ServletContextAttributeEvent(AbstractServletContext.this, key, value);
		if (attributeMap.containsKey(key)) {
			for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
				listener.attributeReplaced(event);
			}
		} else {
			for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
				listener.attributeAdded(event);
			}
		}
		attributeMap.put(key, value);
	}

}
