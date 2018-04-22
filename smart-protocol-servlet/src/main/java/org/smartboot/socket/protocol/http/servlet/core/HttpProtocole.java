package org.smartboot.socket.protocol.http.servlet.core;

/**
 * thanks to RISKO Gergely for the reason phrase patch borrowed from apache
 * 2.2.6: % grep 100\ C -A 65 modules/http/http_protocol.c | grep
 * '^[[:space:]]*"' | cut -d\" -f2 | sed 's/^//;s/ /=/'
 * 
 * @author Jerome Guibert
 */
public enum HttpProtocole {

	HTTP_100("Continue"), HTTP_101("Switching Protocols"), HTTP_102("Processing"), HTTP_200("OK"), HTTP_201("Created"), HTTP_202("Accepted"), HTTP_203("Non-Authoritative Information"), HTTP_204("No Content"), HTTP_205("Reset Content"), HTTP_206(
			"Partial Content"), HTTP_207("Multi-Status"), HTTP_300("Multiple Choices"), HTTP_301("Moved Permanently"), HTTP_302("Found"), HTTP_303("See Other"), HTTP_304("Not Modified"), HTTP_305("Use Proxy"), HTTP_306("unused"), HTTP_307(
			"Temporary Redirect"), HTTP_400("Bad Request"), HTTP_401("Authorization Required"), HTTP_402("Payment Required"), HTTP_403("Forbidden"), HTTP_404("Not Found"), HTTP_405("Method Not Allowed"), HTTP_406("Not Acceptable"), HTTP_407(
			"Proxy Authentication Required"), HTTP_408("Request Time-out"), HTTP_409("Conflict"), HTTP_410("Gone"), HTTP_411("Length Required"), HTTP_412("Precondition Failed"), HTTP_413("Request Entity Too Large"), HTTP_414("Request-URI Too Large"), HTTP_415(
			"Unsupported Media Type"), HTTP_416("Requested Range Not Satisfiable"), HTTP_417("Expectation Failed"), HTTP_418("unused"), HTTP_419("unused"), HTTP_420("unused"), HTTP_421("unused"), HTTP_422("Unprocessable Entity"), HTTP_423("Locked"), HTTP_424(
			"Failed Dependency"), HTTP_425("No code"), HTTP_426("Upgrade Required"), HTTP_500("Internal Server Error"), HTTP_501("Method Not Implemented"), HTTP_502("Bad Gateway"), HTTP_503("Service Temporarily Unavailable"), HTTP_504(
			"Gateway Time-out"), HTTP_505("HTTP Version Not Supported"), HTTP_506("Variant Also Negotiates"), HTTP_507("Insufficient Storage"), HTTP_508("unused"), HTTP_509("unused"), HTTP_510("Not Extended"),HTTP_515("sandao ERROR");

	private final String message;

	HttpProtocole(final String message) {
		this.message = message;
	}

	public final String getMessage() {
		return message;
	}
}
