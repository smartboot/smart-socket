package net.vinote.smart.socket.transport;

import java.io.IOException;

/**
 * Server/Client基本开关功能
 * <p>
 * 为服务器/客户端的传输层提供基本的数据交互服务控制功能
 * </p>
 * <p>
 * 服务的状态{@link ChannelService.Status}需要进行实时监控:
 * <li>Init:ChannelService对象创建即为Init状态</li>
 * <li>STARTING:调用了start()方法后状态立刻切换至STARTING</li>
 * <li>RUNING:服务启动成功后切换至RUNNING</li>
 * <li>STOPPING:调用stop()方法后状态立刻切换至STOPPING</li>
 * <li>STOPPED:服务成功停止运行后切换至STOPPED</li>
 * </p>
 *
 * @author Seer
 * @version ChannelService.java, v 0.1 2015年8月24日 上午10:31:23 Seer Exp.
 */
public interface ChannelService extends Runnable {

	/**
	 *
	 * 停止服务
	 * <p>
	 * 该方法将保持阻塞直至服务关闭成功
	 *
	 * @throws IOException
	 */
	public void shutdown();

	/**
	 * 启动服务
	 * <p>
	 * 该方法需要保持阻塞直至服务启动成功
	 *
	 * @throws IOException
	 */
	public void start() throws IOException;
}
