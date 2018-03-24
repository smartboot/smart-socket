package org.smartboot.socket.http.http11.request;

import org.smartboot.socket.http.HttpResponse;
import org.smartboot.socket.http.handle.HttpHandle;
import org.smartboot.socket.http.handle.StaticResourceHandle;
import org.smartboot.socket.http.http11.Http11Request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public class RouteHandle extends HttpHandle {
    private Map<String, HttpHandle> handleMap = new HashMap<>();
    private StaticResourceHandle defaultHandle;

    public RouteHandle(String baseDir) {
        this.defaultHandle = new StaticResourceHandle(baseDir);
    }

    @Override
    public void doHandle(Http11Request request, HttpResponse response) throws IOException {
        String uri = request.getRequestURI();
        HttpHandle httpHandle = handleMap.get(uri);
        if (httpHandle == null) {
            for (Map.Entry<String, HttpHandle> entity : handleMap.entrySet()) {
                if (uri.matches(entity.getKey())) {
                    httpHandle = entity.getValue();
                    break;
                }
            }
            if (httpHandle == null) {
                httpHandle = defaultHandle;

            }
            handleMap.put(uri, httpHandle);
        }

        httpHandle.doHandle(request, response);
    }

    public void route(String urlPattern, HttpHandle httpHandle) {
        handleMap.put(urlPattern, httpHandle);
    }

}
