package org.smartboot.socket.protocol.http.servlet.api;

import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/31
 */
public class ServletInfo {
    private String servletName;
    private String servletClass;
    private Map<String, String> paramMap;
    private int loadOnStartup;
    private List<String> mappingUrl;

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public String getServletClass() {
        return servletClass;
    }

    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public List<String> getMappingUrl() {
        return mappingUrl;
    }

    public void setMappingUrl(List<String> mappingUrl) {
        this.mappingUrl = mappingUrl;
    }
}
