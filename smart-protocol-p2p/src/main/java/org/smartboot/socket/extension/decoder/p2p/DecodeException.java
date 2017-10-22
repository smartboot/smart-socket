package org.smartboot.socket.extension.decoder.p2p;

/**
 * 消息解码异常
 * 
 * @author 三刀
 * 
 */
public class DecodeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecodeException(String string) {
		super(string);
	}

}
