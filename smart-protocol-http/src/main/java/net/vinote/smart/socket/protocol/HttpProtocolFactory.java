package net.vinote.smart.socket.protocol;

/**
 * Created by zhengjunwei on 2017/6/24.
 */
public class HttpProtocolFactory implements ProtocolFactory<HttpEntity>  {
    @Override
    public Protocol<HttpEntity> createProtocol() {
        return new HttpProtocol();
    }
}
