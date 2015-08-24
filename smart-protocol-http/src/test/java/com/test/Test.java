package com.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.vinote.smart.socket.demo.http.application.DeveloperServerApplication;
import net.vinote.smart.socket.demo.http.server.HttpServerBootstrap;

public class Test {
	public static void main(String[] args) throws Exception {
		System.setProperty("Smart_Home",
				"D:\\zjw\\eclipse\\workspace\\smart-platform\\smart-omc");
		DeveloperServerApplication server = new DeveloperServerApplication();
		server.setServerName("/Test");
		Map<String, HttpServlet> servletMap = new HashMap<String, HttpServlet>();
		servletMap.put("/index.jsp", new HttpServlet() {

			public void init(ServletConfig config) throws ServletException {
				super.init(config);
			}

			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				PrintWriter out = resp.getWriter();
				out.print("<html>");
				out.print("<head><title>Hello World</title></head><body>");
				out.print("<h1>Head</h1>");
				Enumeration<String> headEnum = req.getHeaderNames();
				while (headEnum.hasMoreElements()) {
					String name = headEnum.nextElement();
					out.print(name + ":" + req.getHeader(name) + "</br>");
				}
				out.print("<h1>Parameter</h1>");
				Enumeration<String> paramEnum = req.getParameterNames();
				while (paramEnum.hasMoreElements()) {
					String name = paramEnum.nextElement();
					out.print(name + ":" + req.getParameter(name) + "</br>");
				}
				out.print("<h1>Request Method</h1>");
				out.print("RequestURI:" + req.getRequestURI() + "<br/>");
				out.print("ContextPath:" + req.getContextPath() + "<br/>");
				out.print("ServletPath:" + req.getServletPath() + "<br/>");
				out.print("QueryString:" + req.getQueryString() + "<br/>");
				out.print("RemotePort:" + req.getRemotePort() + "<br/>");
				out.print("RemoteHost:" + req.getRemoteHost() + "<br/>");
				out.print("RemoteAddr:" + req.getRemoteAddr() + "<br/>");
				out.print("</body></html>");
			}
		});
		server.registerServlets(servletMap);
		server.init(null);
		HttpServerBootstrap.start(server);
	}

}
