package net.vinote.smart.socket.protocol.p2p.processor;

import java.nio.ByteBuffer;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.ClusterMessageResp;
import net.vinote.smart.socket.protocol.p2p.message.FragmentMessage;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群消息处理器
 *
 * @author Seer
 * @version ClusterMessageProcessor.java, v 0.1 2015年3月13日 上午10:34:26 Seer Exp.
 */
public class ClusterMessageProcessor extends AbstractServiceMessageProcessor {
	private Logger logger = LoggerFactory.getLogger(ClusterMessageProcessor.class);

	@Override
	public void processor(Session session, DataEntry message) {
		ClusterMessageReq msg = (ClusterMessageReq) message;
		FragmentMessage fragMsg = new FragmentMessage();
		fragMsg.setData(msg.getServiceData());
		BaseMessage baseMsg = fragMsg.decodeMessage(session.getTransportSession().getQuickConfig()
			.getServiceMessageFactory());
		AbstractServiceMessageProcessor processor = session.getTransportSession().getQuickConfig()
			.getServiceMessageFactory().getProcessor(baseMsg.getClass());

		ClusterMessageResp rspMsg = new ClusterMessageResp();
		rspMsg.setUniqueNo(msg.getUniqueNo());
		try {
			ByteBuffer respMesg = processor.processCluster(session, msg.getServiceData());// 由指定消息类型的处理器来处理集群消息
			rspMsg.setSuccess(true);
			rspMsg.setServiceData(respMesg);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			rspMsg.setSuccess(false);
			rspMsg.setInfo(e.getLocalizedMessage());
		}

		try {
			session.sendWithoutResponse(rspMsg);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
