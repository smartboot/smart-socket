package org.smartboot.socket.extension.ssl;

public interface HandshakeCompletionHandler {
    /**
     * 定义 SSL 握手操作完成后的回调处理接口。
     */
    /**
     * 当 SSL 握手操作成功完成时调用此方法。
     *
     * @param attachment 握手操作的相关附件信息，包含握手过程中的状态和数据。
     */
    void completed(HandshakeModel attachment);

    /**
     * 当 SSL 握手操作失败时调用此方法。
     *
     * @param exc        导致握手失败的异常对象，包含失败的详细原因。
     * @param attachment 握手操作的相关附件信息，包含握手过程中的状态和数据。
     */
    void failed(Throwable exc, HandshakeModel attachment);
}