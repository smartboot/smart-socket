/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package org.smartboot.socket.protocol.http.servlet.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.protocol.http.servlet.DispatcherSourceType;
import org.smartboot.socket.protocol.http.servlet.core.authentication.AuthenticationHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.UnavailableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    /**
     * 初始化或者调用getNamedDispatcher获得的Dispatcher执行forward无需设置以下request属性
     * <p>
     * javax.servlet.forward.request_uri
     * javax.servlet.forward.context_path
     * javax.servlet.forward.servlet_path
     * javax.servlet.forward.path_info
     * javax.servlet.forward.query_string
     * </p>
     */
    private DispatcherSourceType sourceType;
    private WebAppConfiguration includedWebAppConfig;
    private ServletConfiguration includedServletConfig;

    /**
     * Constructor. This initializes the filter chain and sets up the details
     * needed to handle a servlet excecution, such as security constraints,
     * filters, etc.
     *
     * @param webAppConfig
     * @param servletConfig
     * @param sourceType    Dispatcher的构建来源
     */
    public SimpleRequestDispatcher(final WebAppConfiguration webAppConfig, final ServletConfiguration servletConfig, DispatcherSourceType sourceType) {
        this.servletConfig = servletConfig;
        this.webAppConfig = webAppConfig;

        matchingFiltersEvaluated = 0;

        this.sourceType = sourceType;

    }

    /**
     * Caches the filter matching, so that if the same URL is requested twice,
     * we don't recalculate the filter matching every time.
     */
    private FilterConfiguration[] getMatchingFilters(final Mapping filterPatterns[], final WebAppConfiguration webAppConfig, final String fullPath, final String servletName, final String filterChainType, final boolean isURLBasedMatch) {

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
    }

    public void setForURLDispatcher(final String servletPath, final String pathInfo, final String queryString, final String requestURIInsideWebapp, final Mapping forwardFilterPatterns[], final Mapping includeFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        requestURI = requestURIInsideWebapp;

        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        matchingFilters = null; // set after the call to forward or include
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
        matchingFilters = getMatchingFilters(errorFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "ERROR", (servletPath != null));
    }

    public void setForInitialDispatcher(final String servletPath, final String pathInfo, final String queryString, final String requestURIInsideWebapp, final Mapping requestFilterPatterns[], final AuthenticationHandler authHandler) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        requestURI = requestURIInsideWebapp;
        this.authHandler = authHandler;
        matchingFilters = getMatchingFilters(requestFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "REQUEST", (servletPath != null));
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
            if (sourceType == DispatcherSourceType.Request_Dispatcher || sourceType == DispatcherSourceType.Error_Dispatcher) {
                final Map<String, Object> includeAttributes = new HashMap<String, Object>();
                if (requestURI != null) {
                    wr.setAttribute(WinstoneConstant.INCLUDE_REQUEST_URI, requestURI);
                }
                if (webAppConfig.getContextPath() != null) {
                    wr.setAttribute(WinstoneConstant.INCLUDE_CONTEXT_PATH, webAppConfig.getContextPath());
                }
                if (servletPath != null) {
                    wr.setAttribute(WinstoneConstant.INCLUDE_SERVLET_PATH, servletPath);
                }
                if (pathInfo != null) {
                    wr.setAttribute(WinstoneConstant.INCLUDE_PATH_INFO, pathInfo);
                }
                if (queryString != null) {
                    wr.setAttribute(WinstoneConstant.INCLUDE_QUERY_STRING, queryString);
                }
            }
            // Add another include buffer to the response stack
            final WinstoneResponse wresp = getUnwrappedResponse(response);
            wresp.setInclude(true);

            includedServletConfig = wr.getServletConfig();
            includedWebAppConfig = wr.getWebAppConfig();
            wr.setServletConfig(servletConfig);
            wr.setWebAppConfig(webAppConfig);
            wresp.setWebAppConfig(webAppConfig);

            doInclude = Boolean.TRUE;
        }

        if (matchingFilters == null) {
            matchingFilters = getMatchingFilters(includeFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "INCLUDE", (servletPath != null));
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
     * <p>RequestDispatcher 接口的 forward 方法，只有在没有输出 交到向客户端时，才能通过正在被调用的 servlet 调用。
     * 如果 response 缓冲区中存在尚未 交的输出数据，这些数据内容必须在目标 servlet 的 service 方法调 用前清除。
     * 如果 response 已经 交，必须抛出一个 IllegalStateException 异常。</p>
     * <p>
     * <p>request 对象暴露给目标 servlet 的路径元素(path elements)必须反映获得 RequestDispatcher 使用的路径。</p>
     * <p>
     * <p>唯一例外的是，如果 RequestDispatcher 是通过 getNamedDispatcher 方法获得。这种情况下，request 对象的路径元素必须反映这些原始请求。</p>
     * <p>
     * <p>在 RequestDispatcher 接口的 forward 方法无异常返回之前，必须发送和 交响应的内容，且由 Servlet 容器 关闭，除非请求处于异步模式。
     * 如果 RequestDispatcher.forward()的目标发生错误，异常信息会传回所有调 用它经过的过滤器和 servlet，且最终传回给容器。</p>
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
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

            // RequestDispatcher 接口的 forward 方法，只有在没有输出提交到向客户端时，才能通过正在被调用的 servlet 调用。
            // 如果 response 缓冲区中存在尚未提交的输出数据，这些数据内容必须在目标 servlet 的 service 方法调用前清除。
            // 如果 response 已经 交，必须抛出一个 IllegalStateException 异常。
            rsp.resetBuffer();
            rsp.setInclude(false);

            // Set request attributes (because it's the first step in the filter
            // chain of a forward or error)
            if (sourceType == DispatcherSourceType.Request_Dispatcher || sourceType == DispatcherSourceType.Error_Dispatcher) {
                req.setAttribute(WinstoneConstant.FORWARD_REQUEST_URI, req.getRequestURI());
                req.setAttribute(WinstoneConstant.FORWARD_CONTEXT_PATH, req.getContextPath());
                req.setAttribute(WinstoneConstant.FORWARD_SERVLET_PATH, req.getServletPath());
                req.setAttribute(WinstoneConstant.FORWARD_PATH_INFO, req.getPathInfo());
                req.setAttribute(WinstoneConstant.FORWARD_QUERY_STRING, req.getQueryString());

                if (sourceType == DispatcherSourceType.Error_Dispatcher) {
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

            // Forwards haven't set up the filter pattern set yet
            if (matchingFilters == null) {
                matchingFilters = getMatchingFilters(forwardFilterPatterns, webAppConfig, servletPath + (pathInfo == null ? "" : pathInfo), getName(), "FORWARD", (servletPath != null));
            } // Otherwise we are an initial or error dispatcher, so check
            // security if initial -
            // if we should not continue, return
            else if (sourceType != DispatcherSourceType.Error_Dispatcher && !continueAfterSecurityCheck(request, response)) {
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
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
            try {
                filter.getFilter().doFilter(request, response, this);
            } catch (final UnavailableException err) {
                throw new ServletException("Error in filter - marking unavailable", err);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
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
