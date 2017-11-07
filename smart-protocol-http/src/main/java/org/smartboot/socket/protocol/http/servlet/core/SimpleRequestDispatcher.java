/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class implements both the SimpleRequestDispatcher and FilterChain
 * components. On the first call to include() or forward(), it starts the filter
 * chain execution if one exists. On the final doFilter() or if there is no
 * chain, we call the include() or forward() again, and the servlet is executed.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SimpleRequestDispatcher.java,v 1.18 2007/04/23 02:55:35
 * rickknowles Exp $
 */
public class SimpleRequestDispatcher implements javax.servlet.RequestDispatcher, javax.servlet.FilterChain {

    protected static Logger logger = LogManager.getLogger(SimpleRequestDispatcher.class);
    private final WebAppConfiguration webAppConfig;
    private final ServletConfiguration servletConfig;
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String requestURI;
    private Integer errorStatusCode;
    private Throwable errorException;
    private String errorSummaryMessage;
    private AuthenticationHandler authHandler;
    private Mapping forwardFilterPatterns[];
    private Mapping includeFilterPatterns[];
    private FilterConfiguration matchingFilters[];
    private int matchingFiltersEvaluated;
    private Boolean doInclude;
    private boolean isErrorDispatch;
    private boolean useRequestAttributes;
    private WebAppConfiguration includedWebAppConfig;
    private ServletConfiguration includedServletConfig;

    /**
     * Constructor. This initializes the filter chain and sets up the details
     * needed to handle a servlet excecution, such as security constraints,
     * filters, etc.
     */
    public SimpleRequestDispatcher(final WebAppConfiguration webAppConfig, final ServletConfiguration servletConfig) {
        this.servletConfig = servletConfig;
        this.webAppConfig = webAppConfig;

        matchingFiltersEvaluated = 0;
    }

    /**
     * Caches the filter matching, so that if the same URL is requested twice,
     * we don't recalculate the filter matching every time.
     */
    private static FilterConfiguration[] getMatchingFilters(final Mapping filterPatterns[], final WebAppConfiguration webAppConfig, final String fullPath, final String servletName, final String filterChainType, final boolean isURLBasedMatch) {

        String cacheKey = null;
        if (isURLBasedMatch) {
            cacheKey = filterChainType + ":URI:" + fullPath;
        } else {
            cacheKey = filterChainType + ":Servlet:" + servletName;
        }
        FilterConfiguration matchingFilters[] = null;
        final Map<String, FilterConfiguration[]> cache = webAppConfig.getFilterMatchCache();
        synchronized (cache) {
            matchingFilters = cache.get(cacheKey);
            if (matchingFilters == null) {
                logger.debug("No cached filter chain available. Calculating for cacheKey={}", cacheKey);
                final List<FilterConfiguration> outFilters = new ArrayList<FilterConfiguration>();
                for (int n = 0; n < filterPatterns.length; n++) {
                    // Get the pattern and eval it, bumping up the eval'd count
                    final Mapping filterPattern = filterPatterns[n];

                    // If the servlet name matches this name, execute it
                    if ((filterPattern.getLinkName() != null) && (filterPattern.getLinkName().equals(servletName) || filterPattern.getLinkName().equals("*"))) {
                        outFilters.add(webAppConfig.getFilters().get(filterPattern.getMappedTo()));
                    } // If the url path matches this filters mappings
                    else if ((filterPattern.getLinkName() == null) && isURLBasedMatch && filterPattern.match(fullPath, null, null)) {
                        outFilters.add(webAppConfig.getFilters().get(filterPattern.getMappedTo()));
                    }
                }
                matchingFilters = outFilters.toArray(new FilterConfiguration[0]);
                cache.put(cacheKey, matchingFilters);
            } else {
                logger.debug("Cached filter chain available for cacheKey={}", cacheKey);
            }
        }
        return matchingFilters;
    }

    public void setForNamedDispatcher(final Mapping forwardFilterPatterns[], final Mapping includeFilterPatterns[]) {
        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        matchingFilters = null; // set after the call to forward or include
        useRequestAttributes = Boolean.FALSE;
        isErrorDispatch = Boolean.FALSE;
    }

    public void setForURLDispatcher(final String servletPath, final String pathInfo, final String queryString, final String requestURIInsideWebapp, final Mapping forwardFilterPatterns[], final Mapping includeFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        requestURI = requestURIInsideWebapp;

        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        matchingFilters = null; // set after the call to forward or include
        useRequestAttributes = Boolean.TRUE;
        isErrorDispatch = Boolean.FALSE;
    }

    public void setForErrorDispatcher(final String servletPath, final String pathInfo, final String queryString, final int statusCode, final String summaryMessage, final Throwable exception, final String errorHandlerURI,
                                      final Mapping errorFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        requestURI = errorHandlerURI;

        errorStatusCode = new Integer(statusCode);
        errorException = exception;
        errorSummaryMessage = summaryMessage;
        matchingFilters = SimpleRequestDispatcher.getMatchingFilters(errorFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "ERROR", (servletPath != null));
        useRequestAttributes = Boolean.TRUE;
        isErrorDispatch = Boolean.TRUE;
    }

    public void setForInitialDispatcher(final String servletPath, final String pathInfo, final String queryString, final String requestURIInsideWebapp, final Mapping requestFilterPatterns[], final AuthenticationHandler authHandler) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        requestURI = requestURIInsideWebapp;
        this.authHandler = authHandler;
        matchingFilters = SimpleRequestDispatcher.getMatchingFilters(requestFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "REQUEST", (servletPath != null));
        useRequestAttributes = Boolean.FALSE;
        isErrorDispatch = Boolean.FALSE;
    }

    public String getName() {
        return servletConfig.getServletName();
    }

    /**
     * Includes the execution of a servlet into the current request Note this
     * method enters itself twice: once with the initial call, and once again
     * when all the filters have completed.
     */
    @Override
    public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {

        // On the first call, log and initialise the filter chain
        if (doInclude == null) {
            logger.debug("INCLUDE: servlet={}, path={}", getName(), requestURI);

            final WinstoneRequest wr = getUnwrappedRequest(request);

            // Set request attributes
            if (useRequestAttributes) {
                wr.addIncludeAttributes(webAppConfig.getContextPath() + requestURI, webAppConfig.getContextPath(), servletPath, pathInfo, queryString);
            }
            // Add another include buffer to the response stack
            final WinstoneResponse wresp = getUnwrappedResponse(response);
            wresp.startIncludeBuffer();

            includedServletConfig = wr.getServletConfig();
            includedWebAppConfig = wr.getWebAppConfig();
            wr.setServletConfig(servletConfig);
            wr.setWebAppConfig(webAppConfig);
            wresp.setWebAppConfig(webAppConfig);

            doInclude = Boolean.TRUE;
        }

        if (matchingFilters == null) {
            matchingFilters = SimpleRequestDispatcher.getMatchingFilters(includeFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "INCLUDE", (servletPath != null));
        }
        try {
            // Make sure the filter chain is exhausted first
            if (matchingFiltersEvaluated < matchingFilters.length) {
                doFilter(request, response);
                finishInclude(request, response);
            } else {
                try {
                    servletConfig.execute(request, response, webAppConfig.getContextPath() + requestURI);
                } finally {
                    if (matchingFilters.length == 0) {
                        finishInclude(request, response);
                    }
                }
            }
        } catch (final Throwable err) {
            finishInclude(request, response);
            if (err instanceof ServletException) {
                throw (ServletException) err;
            } else if (err instanceof IOException) {
                throw (IOException) err;
            } else if (err instanceof Error) {
                throw (Error) err;
            } else {
                throw (RuntimeException) err;
            }
        }
    }

    private void finishInclude(final ServletRequest request, final ServletResponse response) throws IOException {
        final WinstoneRequest wr = getUnwrappedRequest(request);

        // Remove the include buffer from the response stack
        final WinstoneResponse wresp = getUnwrappedResponse(response);
        wresp.finishIncludeBuffer();

        if (includedServletConfig != null) {
            wr.setServletConfig(includedServletConfig);
            includedServletConfig = null;
        }

        if (includedWebAppConfig != null) {
            wr.setWebAppConfig(includedWebAppConfig);
            wresp.setWebAppConfig(includedWebAppConfig);
            includedWebAppConfig = null;
        }
    }

    /**
     * Forwards to another servlet, and when it's finished executing that other
     * servlet, cut off execution. Note this method enters itself twice: once
     * with the initial call, and once again when all the filters have
     * completed.
     */
    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {

        // Only on the first call to forward, we should set any forwarding
        // attributes
        if (doInclude == null) {
            logger.debug("FORWARD: servlet={}, path={}", getName(), requestURI);
            if (response.isCommitted()) {
                throw new IllegalStateException("Called RequestDispatcher.forward() on committed response");
            }

            final WinstoneRequest req = getUnwrappedRequest(request);
            final WinstoneResponse rsp = getUnwrappedResponse(response);

            // Clear the include stack if one has been accumulated
            rsp.resetBuffer();
            rsp.clearIncludeStackForForward();

            // Set request attributes (because it's the first step in the filter
            // chain of a forward or error)
            if (useRequestAttributes) {
                req.setAttribute(WinstoneConstant.FORWARD_REQUEST_URI, req.getRequestURI());
                req.setAttribute(WinstoneConstant.FORWARD_CONTEXT_PATH, req.getContextPath());
                req.setAttribute(WinstoneConstant.FORWARD_SERVLET_PATH, req.getServletPath());
                req.setAttribute(WinstoneConstant.FORWARD_PATH_INFO, req.getPathInfo());
                req.setAttribute(WinstoneConstant.FORWARD_QUERY_STRING, req.getQueryString());

                if (isErrorDispatch) {
                    req.setAttribute(WinstoneConstant.ERROR_REQUEST_URI, req.getRequestURI());
                    req.setAttribute(WinstoneConstant.ERROR_STATUS_CODE, errorStatusCode);
                    req.setAttribute(WinstoneConstant.ERROR_MESSAGE, errorSummaryMessage != null ? errorSummaryMessage : "");
                    if (req.getServletConfig() != null) {
                        req.setAttribute(WinstoneConstant.ERROR_SERVLET_NAME, req.getServletConfig().getServletName());
                    }

                    if (errorException != null) {
                        req.setAttribute(WinstoneConstant.ERROR_EXCEPTION_TYPE, errorException.getClass());
                        req.setAttribute(WinstoneConstant.ERROR_EXCEPTION, errorException);
                    }

                    // Revert back to the original request and response
                    rsp.setErrorStatusCode(errorStatusCode.intValue());
                    request = req;
                    response = rsp;
                }
            }

            req.setServletPath(servletPath);
            req.setPathInfo(pathInfo);
            req.setRequestURI(webAppConfig.getContextPath() + requestURI);
            req.setForwardQueryString(queryString);
            req.setWebAppConfig(webAppConfig);
            req.setServletConfig(servletConfig);
            req.setRequestAttributeListeners(webAppConfig.getRequestAttributeListeners());

            rsp.setWebAppConfig(webAppConfig);

            // Forwards haven't set up the filter pattern set yet
            if (matchingFilters == null) {
                matchingFilters = SimpleRequestDispatcher.getMatchingFilters(forwardFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "FORWARD", (servletPath != null));
            } // Otherwise we are an initial or error dispatcher, so check
            // security if initial -
            // if we should not continue, return
            else if (!isErrorDispatch && !continueAfterSecurityCheck(request, response)) {
                return;
            }

            doInclude = Boolean.FALSE;
        }

        // Make sure the filter chain is exhausted first
        final boolean outsideFilter = (matchingFiltersEvaluated == 0);
        if (matchingFiltersEvaluated < matchingFilters.length) {
            doFilter(request, response);
        } else {
            servletConfig.execute(request, response, webAppConfig.getContextPath() + requestURI);
        }
        // Stop any output after the final filter has been executed (e.g. from
        // forwarding servlet)
        if (outsideFilter) {
            final WinstoneResponse rsp = getUnwrappedResponse(response);
            rsp.flushBuffer();
            rsp.getWinstoneOutputStream().setClosed(Boolean.TRUE);
        }
    }

    private boolean continueAfterSecurityCheck(final ServletRequest request, final ServletResponse response) throws IOException, ServletException {
        // Evaluate security constraints
        if (authHandler != null) {
            return authHandler.processAuthentication(request, response, servletPath + (pathInfo == null ? "" : pathInfo));
        } else {
            return Boolean.TRUE;
        }
    }

    /**
     * Handles the processing of the chain of filters, so that we process them
     * all, then pass on to the main servlet
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
        // Loop through the filter mappings until we hit the end
        while (matchingFiltersEvaluated < matchingFilters.length) {

            final FilterConfiguration filter = matchingFilters[matchingFiltersEvaluated++];
            logger.debug("Executing Filter: {}", filter.getFilterName());
            filter.execute(request, response, this);
            return;
        }

        // Forward / include as requested in the beginning
        if (doInclude == null) {
            return; // will never happen, because we can't call doFilter before
            // forward/include
        } else if (doInclude.booleanValue()) {
            include(request, response);
        } else {
            forward(request, response);
        }
    }

    /**
     * Unwrap back to the original container allocated request object
     */
    protected WinstoneRequest getUnwrappedRequest(final ServletRequest request) {
        ServletRequest workingRequest = request;
        while (workingRequest instanceof ServletRequestWrapper) {
            workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
        }
        return (WinstoneRequest) workingRequest;
    }

    /**
     * Unwrap back to the original container allocated response object
     */
    protected WinstoneResponse getUnwrappedResponse(final ServletResponse response) {
        ServletResponse workingResponse = response;
        while (workingResponse instanceof ServletResponseWrapper) {
            workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
        }
        return (WinstoneResponse) workingResponse;
    }
}
