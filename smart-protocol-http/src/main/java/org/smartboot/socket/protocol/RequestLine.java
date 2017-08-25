package org.smartboot.socket.protocol;

/**
 * Created by seer on 2017/6/27.
 */
public class RequestLine {
    private String protocol;

    private String method;
    private String uri;

    public RequestLine(String protocol, String method, String uri) {
        this.protocol = protocol;
        this.method = method;
        this.uri = uri;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
