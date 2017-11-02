package org.smartboot.socket.protocol.http.process;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.protocol.http.HttpEntity;
import org.smartboot.socket.protocol.http.servlet.HttpClassLoader;
import org.smartboot.socket.protocol.http.servlet.ServletContextImpl;
import org.smartboot.socket.protocol.http.servlet.SmartFilterChainImpl;
import org.smartboot.socket.protocol.http.servlet.api.DeploymentInfo;
import org.smartboot.socket.protocol.http.servlet.api.ServletContainer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StateMachineEnum;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    ServletContainer container = ServletContainer.Factory.newInstance();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public HttpServerMessageProcessor() {

        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setContextPath("/hello");
        deploymentInfo.setClassLoader(new HttpClassLoader());
        deploymentInfo.deploy();
        container.addDeployment(deploymentInfo);
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
        DeploymentInfo deploymentInfo = container.getDeploymentByPath("/hello");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        System.out.println(oldClassLoader);
        Thread.currentThread().setContextClassLoader(deploymentInfo.getClassLoader());
        HttpServletRequest request = deploymentInfo.getRequest();
        HttpServletResponse response = deploymentInfo.getResponse();
        Servlet servlet = deploymentInfo.getMatchServlet(request);

        Filter[] filters = deploymentInfo.getMatchFilters(request);
        SmartFilterChainImpl filterChain = new SmartFilterChainImpl(filters, servlet);
        try {
            filterChain.doFilter(request, response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        Thread.currentThread().setContextClassLoader(oldClassLoader);
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

    }

    @Override
    public void stateEvent(AioSession<HttpEntity> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }

}
