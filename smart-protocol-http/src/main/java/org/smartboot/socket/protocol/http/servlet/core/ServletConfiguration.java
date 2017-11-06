/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the one that keeps a specific servlet instance's config, as well as
 * holding the instance itself.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ServletConfiguration.java,v 1.16 2007/04/23 02:55:35
 * rickknowles Exp $
 */
public class ServletConfiguration implements javax.servlet.ServletConfig, Comparable<ServletConfiguration> {

    private static final transient String ELEM_NAME = "servlet-name";
    // private static final transient String ELEM_DISPLAY_NAME = "display-name";
    private static final transient String ELEM_CLASS = "servlet-class";
    private static final transient String ELEM_JSP_FILE = "jsp-file";
    // private static final transient String ELEM_DESCRIPTION = "description";
    private static final transient String ELEM_INIT_PARAM = "init-param";
    private static final transient String ELEM_INIT_PARAM_NAME = "param-name";
    private static final transient String ELEM_INIT_PARAM_VALUE = "param-value";
    private static final transient String ELEM_LOAD_ON_STARTUP = "load-on-startup";
    private static final transient String ELEM_RUN_AS = "run-as";
    private static final transient String ELEM_SECURITY_ROLE_REF = "security-role-ref";
    private static final transient String ELEM_ROLE_NAME = "role-name";
    private static final transient String ELEM_ROLE_LINK = "role-link";
    private static final transient String JSP_FILE = "org.apache.catalina.jsp_file";
    protected static Logger logger = LogManager.getLogger(ServletConfiguration.class);
    private final Map<String, String> initParams;
    private final WebAppConfiguration webAppConfig;
    // private String runAsRole;
    private final Map<String, String> securityRoleRefs;
    /**
     * runtime memeber
     */
    private final Object servletSemaphore = Boolean.TRUE;
    private String servletName;
    private String className;
    private Servlet instance;
    private int loadOnStartup;
    private String jspFile;
    private boolean isSingleThreadModel = Boolean.FALSE;
    private boolean unavailable = Boolean.FALSE;
    private Throwable unavailableException = null;

    protected ServletConfiguration(final WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        initParams = new HashMap<String, String>();
        loadOnStartup = -1;
        securityRoleRefs = new HashMap<String, String>();
    }

    public ServletConfiguration(final WebAppConfiguration webAppConfig, final String servletName, final String className, final Map<String, String> initParams, final int loadOnStartup) {
        this(webAppConfig);
        if (initParams != null) {
            this.initParams.putAll(initParams);
        }
        this.servletName = servletName;
        this.className = className;
        jspFile = null;
        this.loadOnStartup = loadOnStartup;
    }

    public ServletConfiguration(final WebAppConfiguration webAppConfig, final Node elm) {
        this(webAppConfig);

        // Parse the web.xml file entry
        for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
            final Node child = elm.getChildNodes().item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String nodeName = child.getNodeName();

            // Construct the servlet instances
            if (nodeName.equals(ServletConfiguration.ELEM_NAME)) {
                servletName = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ServletConfiguration.ELEM_CLASS)) {
                className = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ServletConfiguration.ELEM_JSP_FILE)) {
                jspFile = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ServletConfiguration.ELEM_LOAD_ON_STARTUP)) {
                final String index = child.getFirstChild() == null ? "-1" : WebAppConfiguration.getTextFromNode(child);
                loadOnStartup = Integer.parseInt(index);
            } else if (nodeName.equals(ServletConfiguration.ELEM_INIT_PARAM)) {
                String paramName = "";
                String paramValue = "";
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    final Node paramNode = child.getChildNodes().item(k);
                    if (paramNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    } else if (paramNode.getNodeName().equals(ServletConfiguration.ELEM_INIT_PARAM_NAME)) {
                        paramName = WebAppConfiguration.getTextFromNode(paramNode);
                    } else if (paramNode.getNodeName().equals(ServletConfiguration.ELEM_INIT_PARAM_VALUE)) {
                        paramValue = WebAppConfiguration.getTextFromNode(paramNode);
                    }
                }
                if (!paramName.equals("")) {
                    initParams.put(paramName, paramValue);
                }
            } else if (nodeName.equals(ServletConfiguration.ELEM_RUN_AS)) {
                for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                    final Node roleElm = child.getChildNodes().item(m);
                    if ((roleElm.getNodeType() == Node.ELEMENT_NODE) && (roleElm.getNodeName().equals(ServletConfiguration.ELEM_ROLE_NAME))) {
                        // this.runAsRole =
                        // WebAppConfiguration.getTextFromNode(roleElm); // not
                        // used
                    }
                }
            } else if (nodeName.equals(ServletConfiguration.ELEM_SECURITY_ROLE_REF)) {
                String name = "";
                String link = "";
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    final Node roleRefNode = child.getChildNodes().item(k);
                    if (roleRefNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    } else if (roleRefNode.getNodeName().equals(ServletConfiguration.ELEM_ROLE_NAME)) {
                        name = WebAppConfiguration.getTextFromNode(roleRefNode);
                    } else if (roleRefNode.getNodeName().equals(ServletConfiguration.ELEM_ROLE_LINK)) {
                        link = WebAppConfiguration.getTextFromNode(roleRefNode);
                    }
                }
                if (!name.equals("") && !link.equals("")) {
                    initParams.put(name, link);
                }
            }
        }

        if ((jspFile != null) && (className == null)) {
            className = WinstoneConstant.JSP_SERVLET_CLASS;
            WebAppConfiguration.addJspServletParams(initParams);
        }
        ServletConfiguration.logger.debug("Loaded servlet instance {} class: {}", servletName, className);
    }

    @Override
    public String getInitParameter(final String name) {
        return initParams.get(name);
    }

    @Override
    public Enumeration<?> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return webAppConfig;
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    public void ensureInitialization() {

        if (instance != null) {
            return; // already init'd
        }

        synchronized (servletSemaphore) {

            if (instance != null) {
                return; // already init'd
            }

            // Check if we were decommissioned while blocking
            if (unavailableException != null) {
                return;
            }

            // If no instance, class load, then call init()
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());

            Servlet newInstance = null;
            Throwable otherError = null;
            try {
                final Class<?> servletClass = Class.forName(className, Boolean.TRUE, webAppConfig.getLoader());
                newInstance = (Servlet) servletClass.newInstance();
                isSingleThreadModel = Class.forName("javax.servlet.SingleThreadModel").isInstance(newInstance);

                // Initialise with the correct classloader
                ServletConfiguration.logger.debug("{}: init", servletName);
                newInstance.init(this);
                instance = newInstance;
            } catch (final ClassNotFoundException err) {
                ServletConfiguration.logger.warn("Failed to load class: {}", className, err);
                setUnavailable(newInstance);
                unavailableException = err;
            } catch (final IllegalAccessException err) {
                ServletConfiguration.logger.warn("Failed to load class: {}", className, err);
                setUnavailable(newInstance);
                unavailableException = err;
            } catch (final InstantiationException err) {
                ServletConfiguration.logger.warn("Failed to load class: {}", className, err);
                setUnavailable(newInstance);
                unavailableException = err;
            } catch (final ServletException err) {
                ServletConfiguration.logger.warn("Failed to initialise servlet {}", servletName, err);
                instance = null; // so that we don't call the destroy method
                setUnavailable(newInstance);
                unavailableException = err;
            } catch (final RuntimeException err) {
                otherError = err;
                throw err;
            } catch (final Error err) {
                otherError = err;
                throw err;
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
                if ((otherError == null) && (unavailableException == null)) {
                    instance = newInstance;
                }
            }
        }
        return;
    }

    public void execute(final ServletRequest request, final ServletResponse response, final String requestURI) throws ServletException, IOException {

        ensureInitialization();

        // If init failed, return 500 error
        if (unavailable) {
            // ((HttpServletResponse)
            // response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            // resources.getString("StaticResourceServlet.PathNotFound",
            // requestURI));
            final SimpleRequestDispatcher rd = webAppConfig.getErrorDispatcherByClass(unavailableException);
            rd.forward(request, response);
            return;
        }

        if (jspFile != null) {
            request.setAttribute(ServletConfiguration.JSP_FILE, jspFile);
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());

        try {
            if (isSingleThreadModel) {
                synchronized (this) {
                    instance.service(request, response);
                }
            } else {
                instance.service(request, response);
            }
        } catch (final UnavailableException err) {
            // catch locally and rethrow as a new ServletException, so
            // we only invalidate the throwing servlet
            setUnavailable(instance);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND, "File " + requestURI + " not found");
            // throw new ServletException(resources.getString(
            // "SimpleRequestDispatcher.ForwardError"), err);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public Map<String, String> getSecurityRoleRefs() {
        return securityRoleRefs;
    }

    /**
     * This was included so that the servlet instances could be sorted on their
     * loadOnStartup values. Otherwise used.
     */
    @Override
    public int compareTo(final ServletConfiguration objTwo) {
        final Integer one = new Integer(loadOnStartup);
        final Integer two = new Integer(objTwo.loadOnStartup);
        return one.compareTo(two);
    }

    /**
     * Called when it's time for the container to shut this servlet down.
     */
    public void destroy() {
        synchronized (servletSemaphore) {
            setUnavailable(instance);
        }
    }

    protected void setUnavailable(final Servlet unavailableServlet) {

        unavailable = Boolean.TRUE;
        if (unavailableServlet != null) {
            ServletConfiguration.logger.debug("{}: destroy", servletName);
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            try {
                unavailableServlet.destroy();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
                instance = null;
            }
        }

        // remove from webapp
        webAppConfig.removeServletConfigurationAndMappings(this);
    }
}
