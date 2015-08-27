package net.vinote.smart.socket.protocol.p2p.processor;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.enums.ReturnCodeEnum;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthReq;
import net.vinote.smart.socket.protocol.p2p.message.LoginAuthResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 登录鉴权处理器
 * @author Seer
 * @version LoginAuthProcessor.java, v 0.1 2015年8月24日 下午6:44:16 Seer Exp. 
 */
public class LoginAuthProcessor extends AbstractServiceMessageProcessor {
	private static final String SECURE_TOKEN = "SECURE_TOKEN";

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		LoginAuthReq req = (LoginAuthReq) message;
		session.setAttribute(SECURE_TOKEN, "true");
		LoginAuthResp resp = new LoginAuthResp(req.getHead());
		resp.setReturnCode(ReturnCodeEnum.SUCCESS.getCode());
		resp.setReturnDesc(ReturnCodeEnum.SUCCESS.getDesc());
		session.sendWithoutResponse(resp);
	}

}
