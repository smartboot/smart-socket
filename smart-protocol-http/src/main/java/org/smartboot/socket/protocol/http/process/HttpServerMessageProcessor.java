package org.smartboot.socket.protocol.http.process;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.protocol.http.HttpEntity;
import org.smartboot.socket.protocol.http.accesslog.AccessLogger;
import org.smartboot.socket.protocol.http.servlet.core.ClientSocketException;
import org.smartboot.socket.protocol.http.servlet.core.HostConfiguration;
import org.smartboot.socket.protocol.http.servlet.core.SimpleRequestDispatcher;
import org.smartboot.socket.protocol.http.servlet.core.WebAppConfiguration;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneConstant;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneResponse;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StateMachineEnum;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author 三刀
 */
public final class HttpServerMessageProcessor implements MessageProcessor<HttpEntity> {
    private static final Logger LOGGER = LogManager.getLogger(HttpServerMessageProcessor.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public HttpServerMessageProcessor() {

    }

    @Override
    public void process(final AioSession<HttpEntity> session, final HttpEntity entry) {
        //文件上传body部分的数据流需要由业务处理，又不可影响IO主线程
        if (StringUtils.equalsIgnoreCase(entry.getMethod(), "POST")) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        process0(session, entry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            process0(session, entry);
        }
    }

    private void process0(AioSession<HttpEntity> session, HttpEntity entry) {
        ByteBuffer buffer = ByteBuffer.wrap(("HTTP/1.1 200 OK\n" +
                "Server: seer/1.4.4\n" +
                "Content-Length: 2\n" +
                ("Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection")) ?
                        "Connection: keep-alive\n" : ""
                ) +
                "\n" +
                "OK").getBytes());
        try {
            session.write(buffer);
        } catch (IOException e) {
            LOGGER.catching(e);
        }
        if (!"Keep-Alive".equalsIgnoreCase(entry.getHeadMap().get("Connection"))) {
            session.close(false);
        }
        try {
            String servletURI = entry.getUrl();
            WinstoneRequest req = new WinstoneRequest(WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED);
            WinstoneResponse rsp = new WinstoneResponse();
            HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());

            // Get the URI from the request, check for prefix, then
            // match it to a requestDispatcher
            WebAppConfiguration webAppConfig = hostConfig.getWebAppByURI(servletURI);
            if (webAppConfig == null) {
                webAppConfig = hostConfig.getWebAppByURI("/");
            }
            req.setWebAppConfig(webAppConfig);

            // Now we've verified it's in the right webapp, send
            // request in scope notify
            ServletRequestListener reqLsnrs[] = webAppConfig.getRequestListeners();
            for (ServletRequestListener reqLsnr1 : reqLsnrs) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                reqLsnr1.requestInitialized(new ServletRequestEvent(webAppConfig, req));
                Thread.currentThread().setContextClassLoader(cl);
            }

            // Lookup a dispatcher, then process with it
            processRequest(webAppConfig, req, rsp, webAppConfig.getServletURIFromRequestURI(servletURI));
            writeToAccessLog(servletURI, req, rsp, webAppConfig);

            // send request listener notifies
            for (ServletRequestListener reqLsnr : reqLsnrs) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                reqLsnr.requestDestroyed(new ServletRequestEvent(webAppConfig, req));
                Thread.currentThread().setContextClassLoader(cl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }

    }

    private void processRequest(final WebAppConfiguration webAppConfig, final WinstoneRequest req, final WinstoneResponse rsp, final String path) throws IOException, ServletException {
        SimpleRequestDispatcher rd = null;
        javax.servlet.RequestDispatcher rdError = null;
        try {
            rd = webAppConfig.getInitialDispatcher(path, req, rsp);

            // Null RD means an error or we have been redirected to a welcome
            // page
            if (rd != null) {
                LOGGER.debug("Processing with RD: {}", rd.getName());
                rd.forward(req, rsp);
            }
            // if null returned, assume we were redirected
        } catch (final Throwable err) {
            boolean ignore = Boolean.FALSE;
            for (Throwable t = err; t != null; t = t.getCause()) {
                if (t instanceof ClientSocketException) {
                    ignore = Boolean.TRUE;
                    break;
                }
            }
            if (!ignore) {
                LOGGER.warn("Untrapped Error in Servlet", err);
                rdError = webAppConfig.getErrorDispatcherByClass(err);
            }
        }

        // If there was any kind of error, execute the error dispatcher here
        if (rdError != null) {
            try {
                if (rsp.isCommitted()) {
                    rdError.include(req, rsp);
                } else {
                    rsp.resetBuffer();
                    rdError.forward(req, rsp);
                }
            } catch (final Throwable err) {
                LOGGER.error("Error in the error servlet ", err);
            }
            // rsp.sendUntrappedError(err, req, rd != null ? rd.getName() :
            // null);
        }
        rsp.flushBuffer();
//        rsp.getWinstoneOutputStream().setClosed(Boolean.TRUE);
        req.discardRequestBody();
    }

    protected void writeToAccessLog(final String originalURL, final WinstoneRequest request, final WinstoneResponse response, final WebAppConfiguration webAppConfig) {
        if (webAppConfig != null) {
            // Log a row containing appropriate data
            final AccessLogger accessLogger = webAppConfig.getAccessLogger();
            if (accessLogger != null) {
                accessLogger.log(originalURL, request, response);
            }
        }
    }

    @Override
    public void stateEvent(AioSession<HttpEntity> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }

}
