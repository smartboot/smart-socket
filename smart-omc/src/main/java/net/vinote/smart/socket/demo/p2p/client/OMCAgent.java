package net.vinote.smart.socket.demo.p2p.client;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.DetectMessageReq;
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

		// 发送探测消息检测链路是否正常
		int i = 1;
		while (i-- > 0) {
			DetectMessageReq req = new DetectMessageReq();
			DataEntry data = processor.getSession().sendWithResponse(req);
			RunLogger.getLogger().log(Level.SEVERE,
					StringUtils.toHexString(data.getData()));
		}

		// 模拟SOFA的TR服务
		RemoteInterfaceMessageReq req = new RemoteInterfaceMessageReq();
		req.setInterfaceClass(RemoteInterface.class.getName());// 远程接口名称
		req.setMethod("say1");// 调用接口方法
		req.setParamClassList(RemoteModel.class.getName());// 接口方法参数类型
		RemoteModel model = new RemoteModel();
		model.setName("zjw1");
		req.setParams(model);// 参数对象
		RemoteInterfaceMessageResp data = (RemoteInterfaceMessageResp) processor
				.getSession().sendWithResponse(req);
		if (StringUtils.isNotBlank(data.getException())) {
			System.out.println(data.getException());
		} else {
			System.out.println(data.getReturnObject());
		}
		RunLogger.getLogger().log(Level.SEVERE,
				StringUtils.toHexString(data.getData()));
		client.shutdown();
	}
}
