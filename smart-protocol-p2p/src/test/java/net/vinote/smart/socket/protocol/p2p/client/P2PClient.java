package net.vinote.smart.socket.protocol.p2p.client;

import java.util.Properties;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.BaseMessageFactory;
import net.vinote.smart.socket.protocol.p2p.HeartMessageReq;
import net.vinote.smart.socket.protocol.p2p.HeartMessageResp;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.protocol.p2p.server.RemoteInterface;
import net.vinote.smart.socket.protocol.p2p.server.RemoteModel;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.filter.impl.FlowControlFilter;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class P2PClient {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.put(HeartMessageResp.class.getName(), "");
		properties.put(RemoteInterfaceMessageResp.class.getName(), "");
		BaseMessageFactory.getInstance().loadFromProperties(properties);
		QuicklyConfig config = new QuicklyConfig(false);
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setFilters(new SmartFilter[] { new FlowControlFilter() });
		config.setHost("127.0.0.1");
		config.setTimeout(1000);
		NioQuickClient client = new NioQuickClient(config);
		client.start();

		long num = 1;
		long start = System.currentTimeMillis();
		while (num-- > 0) {
			HeartMessageReq req = new HeartMessageReq();
			processor.getSession().sendWithoutResponse(req);
		}
		System.out.println("结束" + (System.currentTimeMillis() - start));

		// 模拟SOFA的TR服务
		RemoteInterfaceMessageReq req = new RemoteInterfaceMessageReq();
		req.setInterfaceClass(RemoteInterface.class.getName());// 远程接口名称
		req.setMethod("say1");// 调用接口方法
		req.setParamClassList(RemoteModel.class.getName());// 接口方法参数类型
		RemoteModel model = new RemoteModel();
		model.setName("zjw1");
		req.setParams(model);// 参数对象
		RemoteInterfaceMessageResp data = (RemoteInterfaceMessageResp) processor.getSession().sendWithResponse(req);
		if (StringUtils.isNotBlank(data.getException())) {
			System.out.println(data.getException());
		} else {
			System.out.println(data.getReturnObject());
		}
		RunLogger.getLogger().log(Level.SEVERE, StringUtils.toHexString(data.getData()));
		client.shutdown();
	}
}
