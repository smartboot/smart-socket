package net.vinote.smart.socket.protocol.p2p;

import java.net.ProtocolException;
import java.util.List;

public class RemoteInterfaceMessageReq extends BaseMessage {

	/** 接口唯一标识 */
	private String uniqueId;

	/** 接口名称 */
	private String interfaceClass;

	/** 调用方法 */
	private String method;

	/** 入参 */
	private List<Object> params;

	@Override
	protected void encodeBody() throws ProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decodeBody() {
		// TODO Auto-generated method stub

	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(String interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}

	@Override
	public int getMessageType() {
		// TODO Auto-generated method stub
		return MessageType.REMOTE_INTERFACE_MESSAGE_REQ;
	}

}
