package net.vinote.smart.socket.demo.http.filter;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import net.vinote.smart.socket.demo.http.application.URLPatternType;

public class SmartServletModel {
	private Map<String, URLPatternType> patternMap;

	private Servlet servlet;

	private ServletConfig config;

	private boolean init = false;

	public SmartServletModel(Map<String, URLPatternType> patternMap,
			Servlet servlet, ServletConfig config, boolean init)
			throws ServletException {
		this.servlet = servlet;
		this.patternMap = patternMap;
		this.config = config;
		this.init = init;
		if (init) {
			this.servlet.init(this.config);
		}
	}

	public final Servlet getServlet() {
		return servlet;
	}

	public final ServletConfig getConfig() {
		return config;
	}

	public final Map<String, URLPatternType> getPatterns() {
		return patternMap;
	}

	public int matches(String url) {
		int index = -1;
		for (Entry<String, URLPatternType> entry : patternMap.entrySet()) {
			switch (entry.getValue()) {
			case AllMatcher:
				if (url.startsWith(entry.getKey())) {
					index = entry.getKey().length();
				}
				break;
			case CatalogMatcher:
			case ExtensionMatcher:
				if (url.matches(entry.getKey())) {
					index = url.length();
				}
				break;
			default:
				throw new IllegalStateException("unsupport state "
						+ entry.getValue());
			}
		}
		return index;
	}
}
