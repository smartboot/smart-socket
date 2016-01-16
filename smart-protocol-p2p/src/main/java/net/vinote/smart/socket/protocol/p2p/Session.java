package net.vinote.smart.socket.protocol.p2p;

import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;

public interface Session {
	public void sendWithoutResponse(BaseMessage requestMsg) throws Exception;

	public BaseMessage sendWithResponse(BaseMessage requestMsg) throws Exception;

	public BaseMessage sendWithResponse(BaseMessage requestMsg, long timeout) throws Exception;

	public boolean notifySyncMessage(BaseMessage baseMsg);
}
