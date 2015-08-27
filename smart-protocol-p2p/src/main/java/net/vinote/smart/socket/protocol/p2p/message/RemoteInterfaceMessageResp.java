package net.vinote.smart.socket.protocol.p2p.message;

import java.net.ProtocolException;

public class RemoteInterfaceMessageResp extends BaseMessage {

	/** 返回对象 */
	private Object returnObject;

	/** 返回对象类型 */
	private String returnType;

	/** 异常 */
	private String exception;

	public RemoteInterfaceMessageResp() {
		super();
	}

	public RemoteInterfaceMessageResp(HeadMessage head) {
		super(head);
	}

	@Override
	protected void encodeBody() throws ProtocolException {
		writeString(exception);
		writeObject(returnObject);
		writeString(returnType);
	}

	@Override
	protected void decodeBody() {
		exception = readString();
		returnObject = readObject();
		returnType = readString();
	}

	public Object getReturnObject() {
		return returnObject;
	}

	public void setReturnObject(Object returnObject) {
		this.returnObject = returnObject;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	@Override
	public int getMessageType() {
		return MessageType.REMOTE_INTERFACE_MESSAGE_RSP;
	}

}
