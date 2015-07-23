package net.vinote.smart.socket.transport;

/**
 * 传输层服务状态
 *
 * @author Seer
 * @version ChannelServiceStatus.java, v 0.1 2015年3月20日 下午2:16:14 Seer Exp.
 */
public enum ChannelServiceStatus {
	/** 初始状态 */
	Init,
	/** 启动中 */
	STARTING,
	/** 运行中 */
	RUNING,
	/** 停止中 */
	STOPPING,
	/** 已停止 */
	STOPPED,
	/** 异常 */
	Abnormal
}