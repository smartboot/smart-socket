package net.vinote.smart.socket.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.vinote.smart.socket.exception.DecodeException;
import net.vinote.smart.socket.lang.SmartByteBuffer;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.http.SmartHttpRequest;

/**
 * 
 * Http消息协议实现
 * 
 * @author Seer
 * 
 */
final class HttpProtocol implements Protocol {

	private SmartHttpRequest httpRequest = new SmartHttpRequest();

	enum ParsePart {
		/** 请求行 */
		RequestLine,
		/** 请求头 */
		HeadLine,
		/** 加载http body */
		BODY_URLENCODED,

		BODY_FORM_DATA,
		/** 终止 */
		End;
	}

	private SmartByteBuffer currentLineSBB;

	private ParsePart part = ParsePart.RequestLine;

	public List<DataEntry> decode(ByteBuffer buffer) {
		List<DataEntry> msgList = new ArrayList<DataEntry>();
		// 未读取到数据则直接返回
		if (buffer == null) {
			return msgList;
		}
		while (buffer.hasRemaining()) {
			switch (part) {
			case RequestLine:
				if (parseRequestLine(buffer)) {
					part = ParsePart.HeadLine;
				}
				break;
			case HeadLine:
				if (parseHeadLine(buffer)) {
					httpRequest.parse();
					msgList.add(httpRequest);
					if ("Post".equalsIgnoreCase(httpRequest.getMethod())) {
						if (httpRequest.getContentType().startsWith(
								"application/x-www-form-urlencoded")) {
							part = ParsePart.BODY_URLENCODED;
						}
						if (httpRequest.getContentType().startsWith(
								"multipart/form-data")) {
							part = ParsePart.BODY_FORM_DATA;
						}
					}
				}
				break;
			case BODY_URLENCODED:// 普通form表单Post提交
				if (!httpRequest.hasParsedParameters()) {
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					httpRequest.getPostFormBuffer().append(bytes);
					part = ParsePart.End;
				}
				break;
			case BODY_FORM_DATA:// 文件上传
				throw new UnsupportedOperationException(
						"unsupport file upload now!");
			default:
				throw new DecodeException("invalid part " + part);
			}
		}
		return msgList;
	}

	/**
	 * 解析消息头
	 * 
	 * @param buffer
	 * @return
	 */
	private boolean parseHeadLine(ByteBuffer buffer) {
		String methodLine = readLine(buffer);
		if (methodLine == null) {
			return false;
		}

		if ("\r\n".equals(methodLine)) {
			return true;
		}

		int index = methodLine.indexOf(":");
		if (index == -1) {
			throw new DecodeException("invalid head line [" + methodLine + "]");
		}

		//
		String name = methodLine.substring(0, index);
		String value = methodLine.substring(index + 1);
		httpRequest.addHeader(name, value);
		return false;
	}

	private boolean parseRequestLine(ByteBuffer buffer) {
		String methodLine = readLine(buffer);
		if (methodLine == null) {
			return false;
		}
		String[] array = methodLine.split(" ");
		if (array.length != 3) {
			throw new DecodeException("invalid request line [" + methodLine
					+ "]");
		}
		RunLogger.getLogger().log(Level.SEVERE, methodLine);
		httpRequest.setMethod(array[0]);
		int index = -1;
		if ((index = array[1].indexOf('?')) > 0) {
			httpRequest.setRequestURI(array[1].substring(0, index));
			httpRequest.setQueryString((array[1].substring(index + 1,
					array[1].length())));
		} else {
			httpRequest.setRequestURI(array[1]);
		}
		httpRequest.setProtocol(array[2]);
		return true;
	}

	private String readLine(ByteBuffer buffer) {
		if (currentLineSBB == null || isEndLine(currentLineSBB)) {
			currentLineSBB = new SmartByteBuffer();
		}
		while (buffer.hasRemaining() && !isEndLine(currentLineSBB)) {
			currentLineSBB.append(buffer.get());
		}
		return isEndLine(currentLineSBB) ? currentLineSBB.toString() : null;
	}

	private boolean isEndLine(SmartByteBuffer buffer) {
		return buffer.length() >= 2
				&& buffer.byteAt(buffer.length() - 2) == '\r'
				&& buffer.byteAt(buffer.length() - 1) == '\n';
	}

	public DataEntry wrapInvalidProtocol() {
		return new SmartHttpRequest();
	}
}
