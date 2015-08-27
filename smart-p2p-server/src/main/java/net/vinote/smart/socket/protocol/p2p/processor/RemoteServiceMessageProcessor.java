package net.vinote.smart.socket.protocol.p2p.processor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.RemoteInterfaceMessageResp;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;

/**
 * 调用远程服务消息处理器
 * 
 * @author Seer
 * @version RemoteServiceMessageProcessor.java, v 0.1 2015年8月21日 下午5:48:43 Seer
 *          Exp.
 */
public class RemoteServiceMessageProcessor extends
		AbstractServiceMessageProcessor {
	private Map<String, Object> impMap = new HashMap<String, Object>();

	@Override
	public void init() {
		super.init();
	}

	@Override
	public void processor(Session session, DataEntry message) throws Exception {
		RemoteInterfaceMessageReq req = (RemoteInterfaceMessageReq) message;
		RemoteInterfaceMessageResp resp = new RemoteInterfaceMessageResp(
				req.getHead());
		try {
			String[] paramClassList = req.getParamClassList();
			Object[] paramObjList = req.getParams();
			// 获取入参类型
			Class<?>[] classArray = null;
			if (paramClassList != null) {
				classArray = new Class[paramClassList.length];
				for (int i = 0; i < classArray.length; i++) {
					classArray[i] = Class.forName(paramClassList[i]);
				}
			}
			// 调用接口
			Object impObj = impMap.get(req.getInterfaceClass());
			Method method = impObj.getClass().getMethod(req.getMethod(),
					classArray);
			Object obj = method.invoke(impObj, paramObjList);
			resp.setReturnObject(obj);
			resp.setReturnType(method.getReturnType().getName());
		} catch (Exception e) {
			RunLogger.getLogger().log(e);
			resp.setException(e.toString());
		}
		session.sendWithoutResponse(resp);
	}
}
