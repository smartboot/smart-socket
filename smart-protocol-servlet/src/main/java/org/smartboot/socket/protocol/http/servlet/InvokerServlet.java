/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.Mapping;
import org.smartboot.socket.protocol.http.servlet.core.ServletConfiguration;
import org.smartboot.socket.protocol.http.servlet.core.SimpleRequestDispatcher;
import org.smartboot.socket.protocol.http.servlet.core.WebAppConfiguration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * If a URI matches a servlet class name, mount an instance of that servlet, and
 * try to process the request using that servlet.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: InvokerServlet.java,v 1.6 2006/03/24 17:24:24 rickknowles Exp $
 */
public class InvokerServlet extends HttpServlet {

    private static final long serialVersionUID = -2502687199563269260L;
    // private static final String FORWARD_PATH_INFO =
    // "javax.servlet.forward.path_info";
    private static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    protected static Logger logger = LogManager.getLogger(InvokerServlet.class);
    private final Boolean mountedInstancesSemaphore = Boolean.TRUE;
    private Map<String, ServletConfiguration> mountedInstances;

    /**
     * Set up a blank map of servlet configuration instances
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        mountedInstances = new Hashtable<String, ServletConfiguration>();
    }

    /**
     * Destroy any mounted instances we might be holding, then destroy myself
     */
    @Override
    public void destroy() {
        if (mountedInstances != null) {
            synchronized (mountedInstancesSemaphore) {
                for (final Iterator<ServletConfiguration> i = mountedInstances.values().iterator(); i.hasNext(); ) {
                    i.next().destroy();
                }
                mountedInstances.clear();
            }
        }
        mountedInstances = null;
    }

    /**
     * Get an instance of the servlet configuration object
     */
    protected ServletConfiguration getInvokableInstance(final String servletName) throws ServletException, IOException {
        ServletConfiguration sc = null;
        synchronized (mountedInstancesSemaphore) {
            if (mountedInstances.containsKey(servletName)) {
                sc = mountedInstances.get(servletName);
            }
        }

        if (sc == null) {
            // If found, mount an instance
            try {
                sc = new ServletConfiguration((WebAppConfiguration) getServletContext(), getServletConfig().getServletName() + ":" + servletName, servletName, new Hashtable<String, String>(), -1);
                mountedInstances.put(servletName, sc);
                InvokerServlet.logger.debug("{}: Mounting servlet class {}", servletName, getServletConfig().getServletName());
                // just to trigger the servlet.init()
                sc.ensureInitialization();
            } catch (final Throwable err) {
                sc = null;
            }
        }
        return sc;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
        final boolean isInclude = (req.getAttribute(InvokerServlet.INCLUDE_PATH_INFO) != null);
        // boolean isForward = (req.getAttribute(FORWARD_PATH_INFO) != null);
        String servletName = null;

        if (isInclude) {
            servletName = (String) req.getAttribute(InvokerServlet.INCLUDE_PATH_INFO);
        } // else if (isForward)
        // servletName = (String) req.getAttribute(FORWARD_PATH_INFO);
        else if (req.getPathInfo() != null) {
            servletName = req.getPathInfo();
        } else {
            servletName = "";
        }
        if (servletName.startsWith("/")) {
            servletName = servletName.substring(1);
        }
        final ServletConfiguration invokedServlet = getInvokableInstance(servletName);

        if (invokedServlet == null) {
            final String msg = "There was no invokable servlet found matching the URL: " + servletName;
            InvokerServlet.logger.warn(msg);
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        } else {
            final SimpleRequestDispatcher rd = new SimpleRequestDispatcher((WebAppConfiguration) getServletContext(), invokedServlet,DispatcherSourceType.Named_Dispatcher);
            rd.setForNamedDispatcher(new Mapping[0], new Mapping[0]);
            rd.forward(req, rsp);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
        doGet(req, rsp);
    }
}
