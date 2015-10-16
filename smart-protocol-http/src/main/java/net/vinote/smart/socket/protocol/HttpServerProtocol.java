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
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.IoBuffer;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;

public class HttpServerProtocol implements Protocol {
	private static final int BUFFER_SIZE = 64;
	private List<DataEntry> msgList = new ArrayList<DataEntry>(BUFFER_SIZE);
	/** Key for decoder current state */
    private static final String DECODER_STATE_ATT = "http.ds";

    /** Key for the partial HTTP requests head */
    private static final String PARTIAL_HEAD_ATT = "http.ph";

    /** Key for the number of bytes remaining to read for completing the body */
    private static final String BODY_REMAINING_BYTES = "http.brb";

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");

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
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(":");

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
	        DecoderState state = (DecoderState) session.getAttribute(DECODER_STATE_ATT);
	        if (null == state) {
	            session.put(DECODER_STATE_ATT, DecoderState.NEW);
	            state = (DecoderState) session.get(DECODER_STATE_ATT);
	        }
	        switch (state) {
	        case HEAD:
	            // grab the stored a partial HEAD request
	            final ByteBuffer oldBuffer = (ByteBuffer) session.get(PARTIAL_HEAD_ATT);
	            // concat the old buffer and the new incoming one
	            // now let's decode like it was a new message
	            msg = IoBuffer.allocate(oldBuffer.remaining() + msg.remaining()).put(oldBuffer).put(msg).flip();
	        case NEW:
	            HttpRequestImpl rq = parseHttpRequestHead(msg.buf());

	            if (rq == null) {
	                // we copy the incoming BB because it's going to be recycled by the inner IoProcessor for next reads
	                ByteBuffer partial = ByteBuffer.allocate(msg.remaining());
	                partial.put(msg.buf());
	                partial.flip();
	                // no request decoded, we accumulate
	                session.put(PARTIAL_HEAD_ATT, partial);
	                session.put(DECODER_STATE_ATT, DecoderState.HEAD);
	                break;
	            } else {
	                out.write(rq);
	                // is it a request with some body content ?
	                String contentLen = rq.getHeader("content-length");

	                if (contentLen != null) {
	                    session.put(BODY_REMAINING_BYTES, Integer.valueOf(contentLen));
	                    session.put(DECODER_STATE_ATT, DecoderState.BODY);
	                    // fallthrough, process body immediately
	                } else {
	                    session.put(DECODER_STATE_ATT, DecoderState.NEW);
	                    out.write(new HttpEndOfContent());
	                    break;
	                }
	            }

	        case BODY:
	            final int chunkSize = msg.remaining();
	            // send the chunk of body
	            if (chunkSize != 0) {
	                final IoBuffer wb = IoBuffer.allocate(msg.remaining());
	                wb.put(msg);
	                wb.flip();
	                out.write(wb);
	            }
	            msg.position(msg.limit());
	            // do we have reach end of body ?
	            int remaining = (Integer) session.get(BODY_REMAINING_BYTES);
	            remaining -= chunkSize;

	            if (remaining <= 0) {
	                session.put(DECODER_STATE_ATT, DecoderState.NEW);
	                session.remove(BODY_REMAINING_BYTES);
	                out.write(new HttpEndOfContent());
	            } else {
	                session.put(BODY_REMAINING_BYTES, Integer.valueOf(remaining));
	            }

	            break;

	        default:
	            throw new HttpException(HttpStatus.CLIENT_ERROR_BAD_REQUEST, "Unknonwn decoder state : " + state);
	        }
	    }
		return msgList;
	}

	@Override
	public DataEntry wrapInvalidProtocol() {
		// TODO Auto-generated method stub
		return null;
	}
	private HttpRequestImpl parseHttpRequestHead(final ByteBuffer buffer) {
        // Java 6 >> String raw = new String(buffer.array(), 0, buffer.limit(), Charset.forName("UTF-8"));
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
            generalHeaders.put(header[0].toLowerCase(), header[1].trim());
        }

        final String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        final HttpMethod method = HttpMethod.valueOf(elements[0]);
        final HttpVersion version = HttpVersion.fromString(elements[2]);
        final String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        final String requestedPath = pathFrags[0];
        final String queryString = pathFrags.length == 2 ? pathFrags[1] : "";

        // we put the buffer position where we found the beginning of the HTTP body
        buffer.position(headersAndBody[0].length() + 4);

        return new HttpRequestImpl(version, method, requestedPath, queryString, generalHeaders);
    }
}
