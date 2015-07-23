package net.vinote.smart.socket.demo.p2p.client;

import java.util.logging.Level;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.DetectMessageReq;
import net.vinote.smart.socket.protocol.p2p.HeadMessage;
import net.vinote.smart.socket.protocol.p2p.client.P2PClientMessageProcessor;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class OMCAgent {
	public static void main(String[] args) throws Exception {
		QuicklyConfig config = new QuicklyConfig();
		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setHost("127.0.0.1");
		config.setTimeout(100);
		NioQuickClient client = new NioQuickClient(config);
		client.start();
		DetectMessageReq req = new DetectMessageReq();
		req.setHead(new HeadMessage());
		DataEntry data = processor.getSession().sendWithResponse(req);
		RunLogger.getLogger().log(Level.SEVERE, StringUtils.toHexString(data.getData()));
		client.shutdown();
	}
}
