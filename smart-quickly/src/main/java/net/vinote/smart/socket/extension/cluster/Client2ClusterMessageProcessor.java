package net.vinote.smart.socket.extension.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.extension.cluster.balance.LoadBalancing;
import net.vinote.smart.socket.extension.timer.QuickTimerTask;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.transport.TransportSession;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

/**
 * 集群服务管理器,被绑定了当前处理器的客户端消息将由该处理器发送至集群服务器
 *
 * @author Seer
 *
 */
public class Client2ClusterMessageProcessor extends AbstractProtocolDataProcessor {
	public static Client2ClusterMessageProcessor getInstance() {
		if (instance == null) {
			synchronized (Client2ClusterMessageProcessor.class) {
				if (instance == null) {
					instance = new Client2ClusterMessageProcessor();
				}
			}
		}
		return instance;
	}

	private static final RunLogger logger = RunLogger.getLogger();
	private static Client2ClusterMessageProcessor instance;
	private ArrayBlockingQueue<ProcessUnit> msgQueue;
	private ClusterServiceProcessThread processThread;
	private ConcurrentMap<String, ProcessUnit> clientTransSessionMap;

	/**
	 * 客户端链接监控
	 */
	private QuickTimerTask sessionMonitorTask;

	private Client2ClusterMessageProcessor() {
	}

	/**
	 * 创建集群服务配置对象
	 *
	 * @param baseConfig
	 * @param url
	 * @return
	 */
	private QuicklyConfig createClusterQuickConfig(final QuicklyConfig baseConfig, final QuickURL url) {
		QuicklyConfig config = new QuicklyConfig(baseConfig.isServer());
		config.setCacheSize(baseConfig.getCacheSize());
		config.setProtocolFactory(baseConfig.getProtocolFactory());
		config.setAutoRecover(true);
		config.setFilters(baseConfig.getFilters());
		config.setQueueOverflowStrategy(baseConfig.getQueueOverflowStrategy());
		config.setLocalIp(baseConfig.getLocalIp());
		config.setHost(url.getIp());
		config.setPort(url.getPort());
		config.setProcessor(new Cluster2ClientMessageProcessor());
		return config;
	}

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName() + " is unsupport current operation!");
	}

	/**
	 * 获取客户端链接对象
	 *
	 * @param clientUniqueNo
	 * @return
	 */
	public TransportSession getClientTransportSession(String clientUniqueNo) {
		ProcessUnit unit = clientTransSessionMap.get(clientUniqueNo);
		return unit == null ? null : unit.clientSession;
	}

	@Override
	public void init(final QuicklyConfig baseConfig) throws Exception {
		super.init(baseConfig);
		String[] clusterUrls = baseConfig.getClusterUrl();
		if (clusterUrls != null && clusterUrls.length > 0) {
			List<QuickURL> urlList = new ArrayList<QuickURL>(clusterUrls.length);
			for (String url : baseConfig.getClusterUrl()) {
				urlList.add(new QuickURL(url.split(":")[0], Integer.valueOf(url.split(":")[1])));
			}

			if (urlList.size() > 0) {
				for (final QuickURL url : urlList) {
					// 构造集群配置
					QuicklyConfig config = createClusterQuickConfig(baseConfig, url);

					NioQuickClient client = new NioQuickClient(config);
					client.start();
					logger.log(Level.SEVERE, "Connect to Cluster Server[ip:" + url.getIp() + " ,port:" + url.getPort()
						+ "]");
					getQuicklyConfig().getLoadBalancing().registServer(client);
				}
			}
		}
		clientTransSessionMap = new ConcurrentHashMap<String, ProcessUnit>();
		msgQueue = new ArrayBlockingQueue<ProcessUnit>(baseConfig.getCacheSize());
		processThread = new ClusterServiceProcessThread("Quickly-Cluster-Process-Thread", this, msgQueue);
		processThread.start();

		// 定时扫描客户端链路有效性
		sessionMonitorTask = new QuickTimerTask() {

			@Override
			protected long getPeriod() {
				return TimeUnit.SECONDS.toMillis(30);
			}

			@Override
			public void run() {
				for (String key : clientTransSessionMap.keySet()) {
					ProcessUnit unit = clientTransSessionMap.get(key);
					if (!unit.clientSession.isValid()) {
						unit.clientSession.close();
						clientTransSessionMap.remove(key);
						logger.log(Level.SEVERE, "remove invalid client[IP:" + unit.clientSession.getRemoteAddr()
							+ " ,Port:" + unit.clientSession.getRemotePort() + "]");
					}
				}
			}

		};
	}

	/**
	 * 仅负责消息分发,集群服务器的响应消息由各连接器处理
	 */

	public <T> void process(T processUnit) {
		ProcessUnit unit = (ProcessUnit) processUnit;
		// 指定集群服务器
		try {
			unit.clusterSession.write((DataEntry) unit.msg);
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		} catch (CacheFullException e) {
			e.printStackTrace();
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 *
	 * 接受客户端消息以便转发至集群服务器
	 **/
	public boolean receive(TransportSession session, DataEntry msg) {
		if (!clientTransSessionMap.containsKey(session.getSessionID())) {
			clientTransSessionMap.put(session.getSessionID(), new ProcessUnit(session, null, null));
		}
		TransportSession clusterSession = clientTransSessionMap.get(session.getSessionID()).clusterSession;
		// 分配集群服务器
		if (clusterSession == null) {
			clusterSession = getQuicklyConfig().getLoadBalancing().balancing(session);
			clientTransSessionMap.get(session.getSessionID()).clusterSession = clusterSession;
		}
		ClusterMessageEntry clusterReq = getQuicklyConfig().getProcessor().generateClusterMessage(msg);// 封装集群消息
		clusterReq.setUniqueNo(session.getSessionID());
		return msgQueue.offer(new ProcessUnit(session, clusterSession, clusterReq));
	}

	public void shutdown() {
		if (processThread != null) {
			processThread.shutdown();
		}
		if (sessionMonitorTask != null) {
			sessionMonitorTask.cancel();
		}
		if (getQuicklyConfig() != null) {
			LoadBalancing load = getQuicklyConfig().getLoadBalancing();
			if (load != null) {
				load.shutdown();
			}
		}
	}
}
