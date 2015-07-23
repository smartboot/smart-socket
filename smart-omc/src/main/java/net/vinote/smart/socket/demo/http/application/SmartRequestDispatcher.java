package net.vinote.smart.socket.demo.http.application;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SmartRequestDispatcher implements RequestDispatcher {
	private String path;
	private AbstractServletContext context;

	public SmartRequestDispatcher(String path, AbstractServletContext context) {
		this.path = path;
		this.context = context;
	}

	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		context.service(request, response, path);
	}

	public void include(ServletRequest arg0, ServletResponse arg1)
			throws ServletException, IOException {

	}

}
