package net.vinote.smart.socket.demo.p2p.client;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.DetectMessageResp;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.protocol.p2p.client.P2PClientMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.processor.RemoteInterface;
import net.vinote.smart.socket.protocol.p2p.processor.RemoteModel;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class OMCAgent {
	public static void main(String[] args) throws Exception {
		QuicklyConfig config = new QuicklyConfig(false);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setHost("127.0.0.1");
		config.setTimeout(100);
		NioQuickClient client = new NioQuickClient(config);
		Properties msgProcessorPro = new Properties();
		msgProcessorPro.put(DetectMessageResp.class.getName(), "");
		msgProcessorPro.put(RemoteInterfaceMessageResp.class.getName(), "");
		BaseMessageFactory.getInstance().loadFromProperties(msgProcessorPro);
		client.start();
		/*
		 * int i = Integer.MAX_VALUE; while (i-- > 0) { DetectMessageReq req =
		 * new DetectMessageReq(); DataEntry data =
		 * processor.getSession().sendWithResponse(req);
		 * RunLogger.getLogger().log(Level.SEVERE,
		 * StringUtils.toHexString(data.getData())); }
		 */
		RemoteInterfaceMessageReq req = new RemoteInterfaceMessageReq();
		req.setInterfaceClass(RemoteInterface.class.getName());
		req.setMethod("say11");
		req.setParamClassList(RemoteModel.class.getName());
		RemoteModel model = new RemoteModel();
		model.setName("zjw1");
		req.setParams(model);
		RemoteInterfaceMessageResp data = (RemoteInterfaceMessageResp) processor
				.getSession().sendWithResponse(req);
		RunLogger.getLogger().log(Level.SEVERE,
				StringUtils.toHexString(data.getData()));
		if (StringUtils.isNotBlank(data.getException())) {
			System.out.println(data.getException());
		} else {
			System.out.println(data.getReturnObject());
		}
		client.shutdown();
	}
}
