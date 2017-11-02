package org.smartboot.socket.protocol.http.servlet.api;

import org.smartboot.socket.protocol.http.servlet.ServletContextImpl;

import javax.servlet.Filter;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/31
 */
public class DeploymentInfo {
    private final Map<String, ServletInfo> servlets = new HashMap<>();
    private final Map<String, FilterInfo> filters = new HashMap<>();
    private ClassLoader classLoader;
    private String deploymentName;
    private String displayName;
    private String contextPath;
    private ServletContext servletContext;

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Servlet getMatchServlet(HttpServletRequest request) {
        return new GenericServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
                System.out.println(req.getClass().getClassLoader());
            }
        };
    }

    public Filter[] getMatchFilters(HttpServletRequest request) {
        return null;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public DeploymentInfo setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public HttpServletRequest getRequest() {
        return new org.smartboot.socket.protocol.http.servlet.HttpServletRequestImpl();
    }

    public HttpServletResponse getResponse() {
        return new org.smartboot.socket.protocol.http.servlet.HttpServletResponseImpl();
    }

    public void deploy() {
        servletContext = new ServletContextImpl(this);
    }
}
