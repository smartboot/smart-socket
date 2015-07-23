package net.vinote.smart.socket.demo.http.filter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import net.vinote.smart.socket.lang.SmartEnumeration;

public class SmartServletConfig implements ServletConfig {
	private Map<String, String> parameterMap = new HashMap<String, String>();

	private String servletName;

	private ServletContext servletContext;

	public SmartServletConfig(String servletName, ServletContext context,
			Map<String, String> parameterMap) {
		this.servletName = servletName;
		this.servletContext = context;
		this.parameterMap = parameterMap;
	}

	public String getServletName() {
		return servletName;
	}

	public ServletContext getServletContext() {

		return servletContext;
	}

	public String getInitParameter(String name) {
		return parameterMap.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return new SmartEnumeration<String>(parameterMap.keySet().iterator());
	}

	public final void setInitParameter(String name, String value) {
		parameterMap.put(name, value);
	}

	public final void setServletName(String servletName) {
		this.servletName = servletName;
	}

}
