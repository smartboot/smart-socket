package net.vinote.smart.socket.lang;

import net.vinote.smart.socket.extension.cluster.balance.LoadBalancing;
import net.vinote.smart.socket.extension.cluster.trigger.ClusterTriggerStrategy;
import net.vinote.smart.socket.protocol.ProtocolFactory;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.service.factory.ServiceProcessorFactory;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.process.ProtocolDataProcessor;

/**
 * Quickly服务端/客户端配置信息
 * 
 * @author Seer
 * 
 */
public class QuicklyConfig {

	/** 自动修复链接 */
	private boolean autoRecover = false;

	/** 消息队列缓存大小 */
	private int cacheSize = 256;

	/** 消息体缓存大小,字节 */
	private int dataBufferSize = 1024;

	/** 集群触发策略 */
	private ClusterTriggerStrategy clusterTriggerStrategy;

	/** 集群环境 */
	private String[] clusterUrl;

	/** 远程服务器IP */
	private String host;

	private LoadBalancing loadBalancing;

	/** 本地IP */
	private String localIp;

	/** 服务器消息拦截器 */
	private SmartFilter[] filters;

	/** 服务器端口号 */
	private int port = 8888;

	/** 消息处理器 */
	private ProtocolDataProcessor processor;

	/** 协议工厂 */
	private ProtocolFactory protocolFactory;

	/**业务消息处理器工厂	 */
	private ServiceProcessorFactory serviceProcessorFactory;
	
	/**业务消息存储工厂	 */
	private ServiceMessageFactory serviceMessageFactory;
	
	/** 队列溢出策略[WAIT,DISCARD] */
	private String queueOverflowStrategy = QueueOverflowStrategy.WAIT.name();

	/** 读管道单论循环操作次数 */
	private int readLoopTimes = 5;

	/** 服务器关闭监听端口 */
	private int shutdownPort = 8005;

	/** 服务器处理线程数 */
	private int threadNum = Runtime.getRuntime().availableProcessors();

	/** 超时时间 */
	private int timeout = Integer.MAX_VALUE;

	/** 写管道单论循环操作次数 */
	private int writeLoopTimes = 10;

	/** true:服务器,false:客户端 */
	private boolean serverOrClient;

	/**
	 * @param serverOrClient
	 *            true:服务器,false:客户端
	 */
	public QuicklyConfig(boolean serverOrClient) {
		this.serverOrClient = serverOrClient;
	}

	public final int getCacheSize() {
		return cacheSize;
	}

	public final ClusterTriggerStrategy getClusterTriggerStrategy() {
		return clusterTriggerStrategy;
	}

	public final String[] getClusterUrl() {
		return clusterUrl;
	}

	public final String getHost() {
		return host;
	}

	public final LoadBalancing getLoadBalancing() {
		return loadBalancing;
	}

	public final String getLocalIp() {
		return localIp;
	}

	public final int getPort() {
		return port;
	}

	public final ProtocolDataProcessor getProcessor() {
		return processor;
	}

	public final ProtocolFactory getProtocolFactory() {
		return protocolFactory;
	}

	public final String getQueueOverflowStrategy() {
		return queueOverflowStrategy;
	}

	public final int getReadLoopTimes() {
		return readLoopTimes;
	}

	public final int getShutdownPort() {
		return shutdownPort;
	}

	public final int getThreadNum() {
		return threadNum;
	}

	public final int getTimeout() {
		return timeout;
	}

	public final int getWriteLoopTimes() {
		return writeLoopTimes;
	}

	public final boolean isAutoRecover() {
		return autoRecover;
	}

	public final void setAutoRecover(boolean autoRecover) {
		this.autoRecover = autoRecover;
	}

	public final void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public final void setClusterTriggerStrategy(
			ClusterTriggerStrategy clusterTriggerStrategy) {
		this.clusterTriggerStrategy = clusterTriggerStrategy;
	}

	public final void setClusterUrl(String[] clusterUrl) {
		this.clusterUrl = clusterUrl;
	}

	public final void setHost(String host) {
		this.host = host;
	}

	public final void setLoadBalancing(LoadBalancing loadBalancing) {
		this.loadBalancing = loadBalancing;
	}

	public final void setLocalIp(String localIp) {
		this.localIp = localIp;
	}

	public final SmartFilter[] getFilters() {
		return filters;
	}

	public final void setFilters(SmartFilter[] filters) {
		this.filters = filters;
	}

	public final void setPort(int port) {
		this.port = port;
	}

	public final void setProcessor(ProtocolDataProcessor processor) {
		this.processor = processor;
	}

	public final void setProtocolFactory(ProtocolFactory protocolFactory) {
		this.protocolFactory = protocolFactory;
	}

	public final void setQueueOverflowStrategy(String queueOverflowStrategy) {
		this.queueOverflowStrategy = queueOverflowStrategy;
	}

	public final void setReadLoopTimes(int readLoopTimes) {
		this.readLoopTimes = readLoopTimes;
	}

	public final void setShutdownPort(int shutdownPort) {
		this.shutdownPort = shutdownPort;
	}

	public final void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}

	public final void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public final void setWriteLoopTimes(int writeLoopTimes) {
		this.writeLoopTimes = writeLoopTimes;
	}

	public final boolean isServer() {
		return serverOrClient;
	}

	public final boolean isClient() {
		return !serverOrClient;
	}

	public final int getDataBufferSize() {
		return dataBufferSize;
	}

	public final void setDataBufferSize(int dataBufferSize) {
		this.dataBufferSize = dataBufferSize;
	}

	public ServiceProcessorFactory getServiceProcessorFactory() {
		return serviceProcessorFactory;
	}

	public void setServiceProcessorFactory(ServiceProcessorFactory serviceProcessorFactory) {
		this.serviceProcessorFactory = serviceProcessorFactory;
	}

	public ServiceMessageFactory getServiceMessageFactory() {
		return serviceMessageFactory;
	}

	public void setServiceMessageFactory(ServiceMessageFactory serviceMessageFactory) {
		this.serviceMessageFactory = serviceMessageFactory;
	}
	
}
