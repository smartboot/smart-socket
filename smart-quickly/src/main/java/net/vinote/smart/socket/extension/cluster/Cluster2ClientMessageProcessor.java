package net.vinote.smart.socket.extension.cluster;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import net.vinote.smart.socket.exception.CacheFullException;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractProtocolDataProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 集群服务器响应消息处理器,将集群服务的消息响应值客户端
 *
 * @author Seer
 *
 */
public class Cluster2ClientMessageProcessor extends AbstractProtocolDataProcessor {
	private ArrayBlockingQueue<ProcessUnit> msgQueue;
	private ClusterServiceProcessThread processThread;

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName() + " is unsupport current operation!");
	}

	@Override
	public void init(QuicklyConfig config) throws Exception {
		super.init(config);
		msgQueue = new ArrayBlockingQueue<ProcessUnit>(10240);
		processThread = new ClusterServiceProcessThread("ClusterResponse-Processor-" + hashCode(), this, msgQueue);
		processThread.start();
	}

	public <T> void process(T processUnit) {
		ProcessUnit unit = (ProcessUnit) processUnit;
		if (unit.clientSession != null) {
			try {
				unit.clientSession.write(unit.msg.getServiceData());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CacheFullException e) {
				e.printStackTrace();
			}
		} else {
			AbstractServiceMessageProcessor processor = ServiceProcessorManager.getInstance().getProcessor(
				unit.msg.getServiceData().getClass());
			try {
				processor.processor(null, unit.msg.getServiceData());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 接受集群服务器的消息
	 */

	public boolean receive(TransportSession clusterSession, DataEntry msg) {
		TransportSession clientSession = null;
		// 识别集群业务消息对应的客户端链接
		if (msg instanceof ClusterMessageResponseEntry) {
			ClusterMessageResponseEntry resp = (ClusterMessageResponseEntry) msg;
			clientSession = Client2ClusterMessageProcessor.getInstance().getClientTransportSession(resp.getUniqueNo());
			return msgQueue.offer(new ProcessUnit(clientSession, clusterSession, resp));
		}
		return false;
	}

	public void shutdown() {
		processThread.shutdown();
	}

}
