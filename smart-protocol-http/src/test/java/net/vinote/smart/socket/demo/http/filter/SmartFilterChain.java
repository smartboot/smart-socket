package net.vinote.smart.socket.demo.http.filter;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SmartFilterChain implements FilterChain {
	private List<Filter> filters;

	private int index = 0;

	private Servlet servlet;

	private boolean executed = false;

	public SmartFilterChain(Servlet servlet, List<Filter> filters) {
		this.servlet = servlet;
		this.filters = filters;
	}

	public void doFilter(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {
		if (index < filters.size()) {
			Filter filter = filters.get(index++);
			filter.doFilter(request, response, this);
		}
		if (!executed) {
			if (servlet != null) {
				servlet.service(request, response);
			} else {
				// 找不到servlet,则尝试作为本地资源进行加载
				/*
				 * response.setStatus(404);
				 * response.getWriter().print(serverName + "404");
				 */
			}
			executed = true;
		}
	}
}
