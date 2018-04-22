package org.smartboot.socket.protocol.http;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneConstant;
import org.smartboot.socket.protocol.http.servlet.core.WinstoneRequest;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Http消息解析器,仅解析Header部分即可
 * Created by 三刀 on 2017/6/20.
 */
public class HttpProtocol implements Protocol<WinstoneRequest> {

    @Override
    public WinstoneRequest decode(ByteBuffer buffer, AioSession<WinstoneRequest> session, boolean eof) {
        WinstoneRequest request = null;
        if (session.getAttachment() == null) {
            try {
                request = new WinstoneRequest(WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED,null);
                InetSocketAddress remoteAddress = session.getRemoteAddress();
                InetSocketAddress localAddress = session.getLocalAddress();
                request.setScheme("http");
                request.setServerPort(localAddress.getPort());
                request.setLocalPort(localAddress.getPort());
                request.setLocalAddr(localAddress.getHostString());
                request.setRemoteIP(remoteAddress.getAddress().getHostAddress());
                request.setRemotePort(remoteAddress.getPort());

                request.setServerName(getHostName(localAddress.getAddress()));
                request.setRemoteName(remoteAddress.getHostName());
                request.setLocalAddr(getHostName(localAddress.getAddress()));
                session.setAttachment(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            session.setAttachment(request);
        } else {
            request = (WinstoneRequest) session.getAttachment();
        }
        boolean returnEntity = false;//是否返回HttpEntity
        switch (request.getDecodePart()) {
            case HEAD:
                if (request.delimiterFrameDecoder.decode(buffer)) {
                    request.decodeHead();//消息头解码
                    if (request.getDecodePart() == HttpDecodePart.END) {
                        returnEntity = true;
                        session.setAttachment(null);
                        break;
                    } else if (request.getDecodePart() == HttpDecodePart.BODY) {
                        returnEntity = !request.postDecodeStrategy.waitForBodyFinish();
                    }
                } else {
                    break;
                }
            case BODY:
                if (request.postDecodeStrategy.isDecodeEnd(buffer, request, eof)) {
                    request.setDecodePart(HttpDecodePart.END);
                    returnEntity = request.postDecodeStrategy.waitForBodyFinish();
                    break;
                }
                break;
            default:
                session.setAttachment(null);
        }
        return returnEntity ? request : null;
    }

    @Override
    public ByteBuffer encode(WinstoneRequest httpEntity, AioSession<WinstoneRequest> session) {
        return null;
    }

    private String getHostAddress(final InetAddress adrs) {
        if (adrs instanceof Inet6Address) {
            return '[' + adrs.getHostAddress() + ']';
        } else {
            return adrs.getHostAddress();
        }
    }

    private String getHostName(final InetAddress adrs) {
        if (adrs instanceof Inet6Address) {
            final String n = adrs.getHostName();
            if (n.indexOf(':') >= 0) {
                return '[' + n + ']';
            }
            return n;
        } else {
            return adrs.getHostName();
        }
    }
}
