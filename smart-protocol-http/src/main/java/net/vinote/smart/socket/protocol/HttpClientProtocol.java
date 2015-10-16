package net.vinote.smart.socket.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.mina.http.ArrayUtil;
import org.apache.mina.http.DecoderState;
import org.apache.mina.http.HttpException;
import org.apache.mina.http.IoBuffer;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;

import net.vinote.smart.socket.exception.DecodeException;

public class HttpClientProtocol implements Protocol {
	private static final int BUFFER_SIZE = 64;
	private List<DataEntry> msgList = new ArrayList<DataEntry>(BUFFER_SIZE);
	/** Key for decoder current state */
	private static final String DECODER_STATE_ATT = "http.ds";

	/** Key for the partial HTTP requests head */
	private static final String PARTIAL_HEAD_ATT = "http.ph";

	/** Key for the number of bytes remaining to read for completing the body */
	private static final String BODY_REMAINING_BYTES = "http.brb";

	/** Key for indicating chunked data */
	private static final String BODY_CHUNKED = "http.ckd";

	/** Regex to parse HttpRequest Request Line */
	public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");

	/** Regex to parse HttpRequest Request Line */
	public static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile(" ");

	/** Regex to parse out QueryString from HttpRequest */
	public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");

	/** Regex to parse out parameters from query string */
	public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;");

	/** Regex to parse out key/value pairs */
	public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");

	/** Regex to parse raw headers and body */
	public static final Pattern RAW_VALUE_PATTERN = Pattern.compile("\\r\\n\\r\\n");

	/** Regex to parse raw headers from body */
	public static final Pattern HEADERS_BODY_PATTERN = Pattern.compile("\\r\\n");

	/** Regex to parse header name and value */
	public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(": ");

	/** Regex to split cookie header following RFC6265 Section 5.4 */
	public static final Pattern COOKIE_SEPARATOR_PATTERN = Pattern.compile(";");

	 ByteBuffer partial ;
	 Map<String,Object> session=new HashMap<String, Object>();
	@Override
	public List<DataEntry> decode(ByteBuffer buffer) {
		msgList.clear();
		// 未读取到数据则直接返回
		if (buffer == null) {
			return msgList;
		}
		while (buffer.hasRemaining()) {
			 DecoderState state = (DecoderState)session.get(DECODER_STATE_ATT);
		        if (null == state) {
		            session.put(DECODER_STATE_ATT, DecoderState.NEW);
		            state = (DecoderState)session.get(DECODER_STATE_ATT);
		        }
			 switch (state) {
		        case HEAD:
		            // grab the stored a partial HEAD request
		            final ByteBuffer oldBuffer = (ByteBuffer)session.get(PARTIAL_HEAD_ATT);
		            // concat the old buffer and the new incoming one
		            IoBuffer.allocate(oldBuffer.remaining() + msg.remaining()).put(oldBuffer).put(msg).flip();
		            // now let's decode like it was a new message

		        case NEW:
		            final DefaultHttpResponse rp = parseHttpReponseHead(buffer);

		            if (rp == null) {
		                // we copy the incoming BB because it's going to be recycled by the inner IoProcessor for next reads
		                 partial = ByteBuffer.allocate(buffer.remaining());
		                partial.put(buffer);
		                partial.flip();
		                // no request decoded, we accumulate
		                session.put(PARTIAL_HEAD_ATT, partial);
		                session.put(DECODER_STATE_ATT, DecoderState.HEAD);
		            } else {
		                out.write(rp);
		                // is it a response with some body content ?
		                session.put(DECODER_STATE_ATT, DecoderState.BODY);

		                final String contentLen = rp.getHeader("content-length");

		                if (contentLen != null) {
		                    session.put(BODY_REMAINING_BYTES, Integer.valueOf(contentLen));
		                } else if ("chunked".equalsIgnoreCase(rp.getHeader("transfer-encoding"))) {
		                    session.put(BODY_CHUNKED, Boolean.valueOf("true"));
		                } else if ("close".equalsIgnoreCase(rp.getHeader("connection"))) {
		                  //  session.close(true);
		                } else {
		                    throw new HttpException(HttpStatus.CLIENT_ERROR_LENGTH_REQUIRED, "no content length !");
		                }
		            }

		            break;

		        case BODY:
		            final int chunkSize = buffer.remaining();
		            // send the chunk of body
		            if (chunkSize != 0) {
		                final IoBuffer wb = IoBuffer.allocate(msg.remaining());
		                wb.put(msg);
		                wb.flip();
		                out.write(wb);
		            }
		            msg.position(msg.limit());
		            
		            // do we have reach end of body ?
		            int remaining = 0;
		            
		            // if chunked, remaining is the msg.remaining()
		            if( session.get(BODY_CHUNKED) != null ) {
		                remaining = chunkSize;
		            } else {
		                // otherwise, manage with content-length
		                remaining = (Integer) session.get(BODY_REMAINING_BYTES);
		                remaining -= chunkSize;
		            }

		            if (remaining <= 0 ) {
		                session.put(DECODER_STATE_ATT, DecoderState.NEW);
		                session.remove(BODY_REMAINING_BYTES);
		                if( session.get(BODY_CHUNKED) != null ) {
		                    session.remove(BODY_CHUNKED);
		                }
		                out.write(new HttpEndOfContent());
		            } else {
		                if( session.get(BODY_CHUNKED) == null ) {
		                    session.put(BODY_REMAINING_BYTES, Integer.valueOf(remaining));
		                }
		            }

		            break;

		        default:
		            throw new HttpException(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR, "Unknonwn decoder state : " + state);
		        }
		}
		return msgList;
	}

	private DefaultHttpResponse parseHttpReponseHead(final ByteBuffer buffer) {
		// Java 6 >> String raw = new String(buffer.array(), 0, buffer.limit(),
		// Charset.forName("UTF-8"));
		final String raw = new String(buffer.array(), 0, buffer.limit());
		final String[] headersAndBody = RAW_VALUE_PATTERN.split(raw, -1);
		if (headersAndBody.length <= 1) {
			// we didn't receive the full HTTP head
			return null;
		}

		String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
		headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

		final String requestLine = headerFields[0];
		final Map<String, String> generalHeaders = new HashMap<String, String>();

		for (int i = 1; i < headerFields.length; i++) {
			final String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
			generalHeaders.put(header[0].toLowerCase(), header[1]);
		}

		final String[] elements = RESPONSE_LINE_PATTERN.split(requestLine);
		HttpStatus status = null;
		final int statusCode = Integer.valueOf(elements[1]);
		for (int i = 0; i < HttpStatus.values().length; i++) {
			status = HttpStatus.values()[i];
			if (statusCode == status.code()) {
				break;
			}
		}
		final HttpVersion version = HttpVersion.fromString(elements[0]);

		// we put the buffer position where we found the beginning of the HTTP
		// body
		buffer.position(headersAndBody[0].length() + 4);

		return new DefaultHttpResponse(version, status, generalHeaders);
	}

	@Override
	public DataEntry wrapInvalidProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

}
