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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Corresponds to a filter object in the web app. Holds one instance only.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class FilterConfiguration implements javax.servlet.FilterConfig {

    protected static Logger logger = LogManager.getLogger(FilterConfiguration.class);

    private final String ELEM_NAME = "filter-name";
    // private final String ELEM_DISPLAY_NAME = "display-name";
    private final String ELEM_CLASS = "filter-class";
    // private final String ELEM_DESCRIPTION = "description";
    private final String ELEM_INIT_PARAM = "init-param";
    private final String ELEM_INIT_PARAM_NAME = "param-name";
    private final String ELEM_INIT_PARAM_VALUE = "param-value";
    private final Map<String, String> initParameters = new HashMap<String, String>();
    private final ServletContext context;
    private final ClassLoader loader;
    private final Object filterSemaphore = Boolean.TRUE;
    private boolean unavailableException = Boolean.FALSE;
    private String filterName;
    private String classFile;
    private Filter instance;

    /**
     * Constructor
     */
    public FilterConfiguration(final ServletContext context, final ClassLoader loader, final Node elm) {
        this.context = context;
        this.loader = loader;

        // Parse the web.xml file entry
        for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
            final Node child = elm.getChildNodes().item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String nodeName = child.getNodeName();

            // Construct the servlet instances
            if (nodeName.equals(ELEM_NAME)) {
                filterName = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_CLASS)) {
                classFile = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_INIT_PARAM)) {
                String paramName = null;
                String paramValue = null;
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    final Node paramNode = child.getChildNodes().item(k);
                    if (paramNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    } else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_NAME)) {
                        paramName = WebAppConfiguration.getTextFromNode(paramNode);
                    } else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_VALUE)) {
                        paramValue = WebAppConfiguration.getTextFromNode(paramNode);
                    }
                }
                if ((paramName != null) && (paramValue != null)) {
                    initParameters.put(paramName, paramValue);
                }
            }
        }
        FilterConfiguration.logger.debug("Loaded filter instance {} class: {}]", filterName, classFile);
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public String getInitParameter(final String paramName) {
        return initParameters.get(paramName);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    /**
     * Implements the first-time-init of an instance, and wraps it in a
     * dispatcher.
     */
    public Filter getFilter() throws ServletException {
        synchronized (filterSemaphore) {
            if (isUnavailable()) {
                throw new WinstoneException("This filter has been marked unavailable because of an earlier error");
            } else if (instance == null) {
                try {
                    // Initialise with the correct classloader
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(loader);

                    final Class<?> filterClass = Class.forName(classFile, Boolean.TRUE, loader);
                    final Object object = filterClass.newInstance();
                    FilterConfiguration.logger.debug("{}: assignable {}", filterClass.getName(), Filter.class.isAssignableFrom(object.getClass()));
                    instance = (Filter) object;
                    FilterConfiguration.logger.debug("{}: init", filterName);
                    instance.init(this);
                    Thread.currentThread().setContextClassLoader(cl);
                } catch (final ClassCastException err) {
                    FilterConfiguration.logger.error("Failed to load class: " + classFile, err);
                } catch (final ClassNotFoundException err) {
                    FilterConfiguration.logger.error("Failed to load class: " + classFile, err);
                } catch (final IllegalAccessException err) {
                    FilterConfiguration.logger.error("Failed to load class: " + classFile, err);
                } catch (final InstantiationException err) {
                    FilterConfiguration.logger.error("Failed to load class: " + classFile, err);
                } catch (final ServletException err) {
                    instance = null;
                    if (err instanceof UnavailableException) {
                        setUnavailable();
                    }
                    throw err;
                }
            }
        }
        return instance;
    }

    /**
     * Called when it's time for the container to shut this servlet down.
     */
    public void destroy() {
        synchronized (filterSemaphore) {
            setUnavailable();
        }
    }

    @Override
    public String toString() {
        return "FilterConfiguration[filterName=" + filterName + ", classFile=" + classFile + ']';
    }

    public boolean isUnavailable() {
        return unavailableException;
    }

    protected void setUnavailable() {
        unavailableException = Boolean.TRUE;
        if (instance != null) {
            FilterConfiguration.logger.debug("{}: destroy", filterName);
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);
            instance.destroy();
            Thread.currentThread().setContextClassLoader(cl);
            instance = null;
        }
    }

    public void execute(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws ServletException, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            getFilter().doFilter(request, response, chain);
        } catch (final UnavailableException err) {
            setUnavailable();
            throw new ServletException("Error in filter - marking unavailable", err);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((filterName == null) ? 0 : filterName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return Boolean.TRUE;
        }
        if (obj == null) {
            return Boolean.FALSE;
        }
        if (getClass() != obj.getClass()) {
            return Boolean.FALSE;
        }
        final FilterConfiguration other = (FilterConfiguration) obj;
        if (filterName == null) {
            if (other.filterName != null) {
                return Boolean.FALSE;
            }
        } else if (!filterName.equals(other.filterName)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
