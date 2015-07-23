
package net.vinote.smart.socket.protocol.http;

import net.vinote.smart.socket.transport.TransportSession;

public class RequestUnit
{
	private SmartHttpRequest request;

	private TransportSession transport;

	public RequestUnit(SmartHttpRequest request, TransportSession transport)
	{
		this.request = request;
		this.transport = transport;
	}

	/**
	 * @return the request
	 */
	public SmartHttpRequest getRequest()
	{
		return request;
	}

	/**
	 * @return the transport
	 */
	public TransportSession getTransport()
	{
		return transport;
	}

}
