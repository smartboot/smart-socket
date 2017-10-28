package org.smartboot.socket.protocol.http.servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/27
 */
public class SmartRequestDispatcher implements RequestDispatcher {
    private ServletContext servletContext;
    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
//        servletContext.get
        servletContext.getServlet("").service(request,response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        servletContext.getServlet("").service(request,response);
    }
}
