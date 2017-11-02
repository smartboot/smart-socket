package org.smartboot.socket.protocol.http.servlet;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/28
 */
public class FilterConfigImpl implements FilterConfig {
    private final Map<String, String> initParams = new HashMap<>();
    private String filterName;
    private ServletContext servletContext;

    public FilterConfigImpl(String filterName, Map<String, String> initParams, ServletContext servletContext) {
        this.filterName = filterName;
        this.servletContext = servletContext;
        if (initParams != null) {
            this.initParams.putAll(initParams);
        }
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }
}
