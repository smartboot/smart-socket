package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * 参考规范：https://github.com/haproxy/haproxy/blob/master/doc/proxy-protocol.txt
 *
 * @param <T>
 */
public class ProxyProtocolPlugin<T> extends AbstractPlugin<T> {
    private static final byte AF_UNSPEC_BYTE = 0x00;
    private static final int AF_IPV4_BYTE = 0x10;
    private static final byte AF_IPV6_BYTE = 0x20;
    private static final byte AF_UNIX_BYTE = 0x30;
    private static final byte TP_UNSPEC_BYTE = 0x00;
    private static final byte TP_STREAM_BYTE = 0x01;
    private static final byte TP_DGRAM_BYTE = 0x02;

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new ProxyProtocolChannel(channel);
    }

    static class ProxyProtocolChannel extends AsynchronousSocketChannelProxy {
        private static final byte STATE_READY = 0;
        private static final byte STATE_PROXY_SIGN = 1;
        private static final byte STATE_V2_HEADER = STATE_PROXY_SIGN + 1;
        private static final byte STATE_V2_IPv4 = STATE_V2_HEADER + 1;
        private static final byte STATE_V2_IPv6 = STATE_V2_IPv4 + 1;
        private static final byte STATE_V2_UNIX = STATE_V2_IPv6 + 1;
        private static final byte STATE_V1_TYPE = STATE_V2_UNIX + 1;
        private static final byte STATE_V1_SOURCE_IP = STATE_V1_TYPE + 1;
        private static final byte STATE_V1_DEST_IP = STATE_V1_SOURCE_IP + 1;
        private static final byte STATE_V1_SOURCE_PORT = STATE_V1_DEST_IP + 1;
        private static final byte STATE_V1_DEST_PORT = STATE_V1_SOURCE_PORT + 1;
        private static final byte STATE_V1_SKIP_TO_END = STATE_V1_DEST_PORT + 1;
        private static final byte STATE_END = STATE_V1_SKIP_TO_END + 1;
        private byte state = STATE_PROXY_SIGN;
        private String sourceIp;
        private String destIp;
        private SocketAddress remoteAddress;
        private SocketAddress localAddress;

        public ProxyProtocolChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
            super(asynchronousSocketChannel);
        }

        @Override
        public <A> void read(ByteBuffer buffer, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (state == STATE_READY) {
                super.read(buffer, timeout, unit, attachment, handler);
            } else {
                if (buffer.capacity() < 16) {
                    handler.failed(new IOException("buffer capacity is not enough"), attachment);
                    return;
                }
                super.read(buffer, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
                    @Override
                    public void completed(Integer result, A attachment) {
                        buffer.flip();
                        Exception e = null;
                        e = decodeProxyProtocol(buffer);

                        if (e != null) {
                            handler.failed(e, attachment);
                            return;
                        }
                        buffer.compact();
                        if (state == STATE_READY) {
                            if (buffer.position() > 0) {
                                handler.completed(buffer.position(), attachment);
                            } else {
                                ProxyProtocolChannel.super.read(buffer, timeout, unit, attachment, handler);
                            }
                        } else {
                            ProxyProtocolChannel.super.read(buffer, timeout, unit, attachment, this);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, A attachment) {
                        handler.failed(exc, attachment);
                    }
                });
            }

        }

        private Exception decodeProxyProtocol(ByteBuffer buffer) {
            switch (state) {
                case STATE_PROXY_SIGN: {
                    if (buffer.remaining() < 12) {
                        break;
                    }
                    byte b = buffer.get();
                    if (b == 'P') {
                        if (buffer.get() != 'R' || buffer.get() != 'O' || buffer.get() != 'X' || buffer.get() != 'Y' || buffer.get() != ' ') {
                            return new IOException("not proxy protocol");
                        }
                        state = STATE_V1_TYPE;
                    } else if (b == 0x0D) {
                        //The binary header format starts with a constant 12 bytes block containing the
                        //protocol signature :
                        //
                        //   \x0D \x0A \x0D \x0A \x00 \x0D \x0A \x51 \x55 \x49 \x54 \x0A
                        //
                        //Note that this block contains a null byte at the 5th position, so it must not
                        //be handled as a null-terminated string.
                        if (buffer.get() != 0x0A || buffer.get() != 0x0D || buffer.get() != 0x0A || buffer.get() != 0x00 || buffer.get() != 0x0D || buffer.get() != 0x0A || buffer.get() != 0x51 || buffer.get() != 0x55 || buffer.get() != 0x49 || buffer.get() != 0x54 || buffer.get() != 0x0A) {
                            return new IOException("not proxy protocol");
                        }
                        state = STATE_V2_HEADER;
                    } else {
                        return new IOException("invalid proxy protocol");
                    }
                    return decodeProxyProtocol(buffer);
                }
                case STATE_V1_TYPE: {
                    if (buffer.remaining() < 8) {
                        break;
                    }
                    //对于v1版本来说，支持"TCP4"和"TCP6"这两种方式。
                    //如果要使用其他的协议，那么可以设置为"UNKNOWN"。如果设置为"UNKNOWN"，那么后面到CRLF之前的数据将会被忽略。
                    byte b = buffer.get();
                    if (b == 'T') {
                        if (buffer.get() != 'C' || buffer.get() != 'P') {
                            return new IOException("invalid proxy protocol");
                        }
                        b = buffer.get();
                        if (b == '4') {
//                            ipv4 = true;
                        } else if (b == '6') {
//                            ipv4 = false;
                        } else {
                            return new IOException("invalid proxy protocol");
                        }
                        if (buffer.get() != ' ') {
                            return new IOException("invalid proxy protocol");
                        }
                        state = STATE_V1_SOURCE_IP;
                    } else if (b == 'U') {
                        if (buffer.get() != 'N' || buffer.get() != 'K' || buffer.get() != 'N' || buffer.get() != 'O' || buffer.get() != 'W' || buffer.get() != 'N' || buffer.get() != ' ') {
                            return new IOException("invalid proxy protocol");
                        }
                        state = STATE_V1_SKIP_TO_END;
                    } else {
                        return new IOException("not proxy protocol");
                    }
                    return decodeProxyProtocol(buffer);
                }
                case STATE_V1_SOURCE_IP:
                    sourceIp = getIp(buffer);
                    if (sourceIp == null) {
                        return null;
                    }
                    state = STATE_V1_DEST_IP;
                case STATE_V1_DEST_IP:
                    destIp = getIp(buffer);
                    if (destIp == null) {
                        return null;
                    }
                case STATE_V1_SOURCE_PORT: {
                    int port = getPort(buffer);
                    if (port == -1) {
                        return null;
                    }
                    remoteAddress = new InetSocketAddress(sourceIp, port);
                    state = STATE_V1_DEST_PORT;
                }
                case STATE_V1_DEST_PORT: {
                    int port = getPort(buffer);
                    if (port == -1) {
                        return null;
                    }
                    localAddress = new InetSocketAddress(destIp, port);
                    buffer.position(buffer.position() - 1);
                    state = STATE_END;
                }
                case STATE_END: {
                    if (buffer.remaining() < 2) {
                        return null;
                    }
                    if (buffer.get() != '\r' || buffer.get() != '\n') {
                        return new IOException("invalid proxy protocol");
                    }
                    state = STATE_READY;
                    break;
                }
                case STATE_V1_SKIP_TO_END: {
                    while (buffer.remaining() >= 2) {
                        if (buffer.get() != '\r') {
                            continue;
                        }
                        if (buffer.get() != '\n') {
                            return new IOException("invalid proxy protocol");
                        }
                        state = STATE_READY;
                    }
                    break;
                }
                case STATE_V2_HEADER: {
                    if (buffer.remaining() < 4) {
                        return null;
                    }
                    int b = buffer.get();
                    if (b >> 4 != 0x2) {
                        return new IOException("invalid proxy protocol version");
                    }
                    int cmd = b & 0x0F;

                    b = buffer.get();
                    byte addressFamily = (byte) (b & 0xF0);
                    if (addressFamily > AF_UNIX_BYTE) {
                        return new IOException("invalid proxy protocol address family");
                    }
                    byte transportProtocol = (byte) (b & 0x0F);
                    if (transportProtocol > TP_DGRAM_BYTE) {
                        return new IOException("invalid proxy protocol transport protocol");
                    }

                    int addressLength = buffer.getShort();
                    switch (b) {
                        case AF_UNSPEC_BYTE | TP_UNSPEC_BYTE:
                            break;
                        case AF_IPV4_BYTE | TP_STREAM_BYTE:
                        case AF_IPV4_BYTE | TP_DGRAM_BYTE:
                            state = STATE_V2_IPv4;
                            if (addressLength != 12) {
                                return new IOException("invalid proxy protocol address length");
                            }
                            break;
                        case AF_IPV6_BYTE | TP_STREAM_BYTE:
                        case AF_IPV6_BYTE | TP_DGRAM_BYTE:
                            if (addressLength != 36) {
                                return new IOException("invalid proxy protocol address length");
                            }
                            state = STATE_V2_IPv6;
                            break;
                        case AF_UNIX_BYTE | TP_STREAM_BYTE:
                        case AF_UNIX_BYTE | TP_DGRAM_BYTE:
                            if (addressLength != 216) {
                                return new IOException("invalid proxy protocol address length");
                            }
                            state = STATE_V2_UNIX;
                            break;
                        default:
                            return new IOException("invalid proxy protocol address family");
                    }
                    return decodeProxyProtocol(buffer);
                }
                case STATE_V2_IPv4: {
                    if (buffer.remaining() < 12) {
                        return null;
                    }
                    sourceIp = (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF);
                    destIp = (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF) + "." + (buffer.get() & 0xFF);
                    remoteAddress = new InetSocketAddress(sourceIp, buffer.getShort() & 0xFFFF);
                    localAddress = new InetSocketAddress(destIp, buffer.getShort() & 0xFFFF);
                    state = STATE_READY;
                    break;
                }
                case STATE_V2_IPv6: {
                    if (buffer.remaining() < 36) {
                        return null;
                    }
                    StringBuilder sourceIp = new StringBuilder(Integer.toHexString(buffer.getShort() & 0xFFFF));
                    for (int i = 0; i < 3; i++) {
                        sourceIp.append(":").append(Integer.toHexString(buffer.getShort() & 0xFFFF));
                    }
                    StringBuilder destIp = new StringBuilder(Integer.toHexString(buffer.getShort() & 0xFFFF));
                    for (int i = 0; i < 3; i++) {
                        destIp.append(":").append(Integer.toHexString(buffer.getShort() & 0xFFFF));
                    }
                    remoteAddress = new InetSocketAddress(sourceIp.toString(), buffer.getShort() & 0xFFFF);
                    localAddress = new InetSocketAddress(destIp.toString(), buffer.getShort() & 0xFFFF);
                    state = STATE_READY;
                    break;
                }
                case STATE_V2_UNIX: {
                    if (buffer.remaining() < 216) {
                        return null;
                    }

                }
            }
            return null;
        }


        private String getIp(ByteBuffer buffer) {
            int p = buffer.position();
            buffer.mark();
            boolean ok = false;
            while (buffer.hasRemaining()) {
                if (buffer.get() == ' ') {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                buffer.reset();
                return null;
            }
            byte[] bytes = new byte[buffer.position() - p];
            buffer.reset();
            buffer.get(bytes);
            return new String(bytes, 0, bytes.length - 1);
        }

        private int getPort(ByteBuffer buffer) {
            buffer.mark();
            int port = 0;
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (b < '0' || b > '9') {
                    return port;
                }
                port = port * 10 + b - '0';
            }
            buffer.reset();
            return -1;
        }

        @Override
        public SocketAddress getRemoteAddress() throws IOException {
            checkState();
            return remoteAddress;
        }

        @Override
        public SocketAddress getLocalAddress() throws IOException {
            checkState();
            return localAddress;
        }

        private void checkState() throws IOException {
            if (state != STATE_READY) {
                throw new IOException("proxy protocol not ready");
            }
        }
    }


}
