//package net.vinote.smart.socket.protocol.p2p.processor;
//
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import net.vinote.smart.socket.protocol.p2p.AbstractServiceMessageProcessor;
//import net.vinote.smart.socket.protocol.p2p.Session;
//import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
//
///**
// * 调用远程服务消息处理器
// *
// * @author Seer
// * @version RemoteServiceMessageProcessor.java, v 0.1 2015年8月21日 下午5:48:43 Seer
// *          Exp.
// */
//public class RemoteServiceMessageProcessor extends AbstractServiceMessageProcessor {
//	private Logger logger = LogManager.getLogger(RemoteServiceMessageProcessor.class);
//	private Map<String, Object> impMap = new HashMap<String, Object>();
//
//	public void registService(String key, Object service) {
//		logger.info("注册服务,key:" + key + ",service:" + service.getClass().getName());
//		impMap.put(key, service);
//	}
//
//	@Override
//	public void processor(Session session, BaseMessage message){
//		RemoteInterfaceMessageReq req = (RemoteInterfaceMessageReq) message;
//		RemoteInterfaceMessageResp resp = new RemoteInterfaceMessageResp(req.getHead());
//		try {
//			String[] paramClassList = req.getParamClassList();
//			Object[] paramObjList = req.getParams();
//			// 获取入参类型
//			Class<?>[] classArray = null;
//			if (paramClassList != null) {
//				classArray = new Class[paramClassList.length];
//				for (int i = 0; i < classArray.length; i++) {
//					Class<?> clazz = getPrimitiveClass(paramClassList[i]);
//					if (clazz == null) {
//						classArray[i] = Class.forName(paramClassList[i]);
//					} else {
//						classArray[i] = clazz;
//					}
//				}
//			}
//			// 调用接口
//			Object impObj = impMap.get(req.getInterfaceClass());
//			Method method = impObj.getClass().getMethod(req.getMethod(), classArray);
//			Object obj = method.invoke(impObj, paramObjList);
//			resp.setReturnObject(obj);
//			resp.setReturnType(method.getReturnType().getName());
//		} catch (Exception e) {
//			logger.warn(e.getMessage(), e);
//			resp.setException(e.toString());
//		}
//		session.sendWithoutResponse(resp);
//	}
//
//	private Class<?> getPrimitiveClass(String name) {
//		if ("int".equals(name)) {
//			return int.class;
//		}
//		if ("double".equals(name)) {
//			return double.class;
//		}
//		return null;
//	}
//
//	public static void main(String[] args) {
//		System.out.println(Boolean.TYPE);
//	}
//}
