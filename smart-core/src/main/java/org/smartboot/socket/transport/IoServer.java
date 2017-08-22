package org.smartboot.socket.transport;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Server/Client基本开关功能
 *
 * @author Seer
 * @version ChannelService.java, v 0.1 2015年8月24日 上午10:31:23 Seer Exp.
 */
public interface IoServer {

    /**
     * 停止服务
     * <p>
     * 该方法将保持阻塞直至服务关闭成功
     *
     * @throws IOException
     */
    void shutdown();

    /**
     * 启动服务
     * <p>
     * 该方法需要保持阻塞直至服务启动成功
     *
     * @throws IOException
     */
    void start() throws IOException, ExecutionException, InterruptedException;
}
