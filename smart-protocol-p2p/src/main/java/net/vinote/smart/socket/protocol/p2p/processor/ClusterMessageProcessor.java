package net.vinote.smart.socket.protocol.p2p.processor;

import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.ClusterMessageResp;
import net.vinote.smart.socket.service.manager.ServiceProcessorManager;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 集群消息处理器
 * 
 * @author Seer
 * @version ClusterMessageProcessor.java, v 0.1 2015年3月13日 上午10:34:26 Seer Exp.
 */
public class ClusterMessageProcessor extends AbstractServiceMessageProcessor {

	public void processor(Session session, DataEntry message) {
		ClusterMessageReq msg = (ClusterMessageReq) message;
		AbstractServiceMessageProcessor processor = ServiceProcessorManager
				.getInstance().getProcessor(msg.getServiceData().getClass());

		ClusterMessageResp rspMsg = new ClusterMessageResp();
		rspMsg.setUniqueNo(msg.getUniqueNo());
		try {
			DataEntry respMesg = processor.processCluster(session,
					msg.getServiceData());// 由指定消息类型的处理器来处理集群消息
			rspMsg.setSuccess(true);
			rspMsg.setServiceData(respMesg);
		} catch (Exception e) {
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
			rspMsg.setSuccess(false);
			rspMsg.setInfo(e.getLocalizedMessage());
		}

		try {
			session.sendWithoutResponse(rspMsg);
		} catch (Exception e) {
			RunLogger.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
	}
}
