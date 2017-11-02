package org.smartboot.socket.protocol.http.servlet.api;

import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/31
 */
public class FilterInfo {
    private String filterName;
    private String filterClass;
    private Map<String, String> paramMap;
    private List<String> mappingUrl;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    public List<String> getMappingUrl() {
        return mappingUrl;
    }

    public void setMappingUrl(List<String> mappingUrl) {
        this.mappingUrl = mappingUrl;
    }
}
