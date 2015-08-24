package net.vinote.smart.socket.demo.http.filter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import net.vinote.smart.socket.lang.SmartEnumeration;

public class SmartFilterConfig implements FilterConfig {

	private Map<String, String> config = new HashMap<String, String>();
	private ServletContext context;
	private String filterName;

	public SmartFilterConfig(String filterName, ServletContext context) {
		this.filterName = filterName;
		this.context = context;
	}

	public String getFilterName() {
		return filterName;
	}

	public ServletContext getServletContext() {
		return context;
	}

	public String getInitParameter(String name) {
		return config.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return new SmartEnumeration<String>(config.keySet().iterator());
	}

	public void setParameter(String key, String value) {
		config.put(key, value);
	}
}
