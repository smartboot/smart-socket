package org.smartboot.socket.util;

/**
 * @author 三刀
 * @version V1.0 , 2018/4/22
 */
public class DecoderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DecoderException() {
    }

    public DecoderException(String message) {
        super(message);
    }

    public DecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecoderException(Throwable cause) {
        super(cause);
    }

    public DecoderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
