package org.smartboot.socket.protocol.http.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SmartFilterChainImpl implements FilterChain {
	private Filter[] filters;
	private byte pos = 0;
	private Servlet servlet;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		if (pos < filters.length) {
			Filter filter = filters[pos++];
			filter.doFilter(request, response, this);
		}
		servlet.service(request, response);
	}

}
