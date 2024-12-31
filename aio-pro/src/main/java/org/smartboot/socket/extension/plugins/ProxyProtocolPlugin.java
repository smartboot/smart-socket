package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

public class ProxyProtocolPlugin<T> extends AbstractPlugin<T> {

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new ProxyProtocolChannel(channel);
    }

    static class ProxyProtocolChannel extends AsynchronousSocketChannelProxy {
        private static final byte STATE_READY = 0;
        private static final byte STATE_PROXY = 1;
        private static final byte STATE_PROTOCOL_TYPE = 2;
        private static final byte STATE_PROTOCOL_SOURCE_IP = 3;
        private static final byte STATE_PROTOCOL_DEST_IP = 4;
        private static final byte STATE_PROTOCOL_SOURCE_PORT = 5;
        private static final byte STATE_PROTOCOL_DEST_PORT = 6;
        private static final byte STATE_SKIP_TO_END = 7;
        private static final byte STATE_END = 8;
        private byte state = STATE_PROXY;
        private boolean ipv4;
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
                        Exception e = decodeProxyProtocol(buffer);
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
                case STATE_PROXY:
                    if (buffer.remaining() < 6) {
                        break;
                    }
                    if (buffer.get() != 'P' || buffer.get() != 'R' || buffer.get() != 'O' || buffer.get() != 'X' || buffer.get() != 'Y' || buffer.get() != ' ') {
                        return new IOException("not proxy protocol");
                    }
                    state = STATE_PROTOCOL_TYPE;
                case STATE_PROTOCOL_TYPE:
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
                            ipv4 = true;
                        } else if (b == '6') {
                            ipv4 = false;
                        } else {
                            return new IOException("invalid proxy protocol");
                        }
                        if (buffer.get() != ' ') {
                            return new IOException("invalid proxy protocol");
                        }
                        state = STATE_PROTOCOL_SOURCE_IP;
                    } else if (b == 'U') {
                        if (buffer.get() != 'N' || buffer.get() != 'K' || buffer.get() != 'N' || buffer.get() != 'O' || buffer.get() != 'W' || buffer.get() != 'N' || buffer.get() != ' ') {
                            return new IOException("invalid proxy protocol");
                        }
                        state = STATE_SKIP_TO_END;
                        return decodeProxyProtocol(buffer);
                    } else {
                        return new IOException("not proxy protocol");
                    }
                case STATE_PROTOCOL_SOURCE_IP:
                    sourceIp = getIp(buffer);
                    if (sourceIp == null) {
                        return null;
                    }
                    state = STATE_PROTOCOL_DEST_IP;
                case STATE_PROTOCOL_DEST_IP:
                    destIp = getIp(buffer);
                    if (destIp == null) {
                        return null;
                    }
                case STATE_PROTOCOL_SOURCE_PORT: {
                    int port = getPort(buffer);
                    if (port == -1) {
                        return null;
                    }
                    remoteAddress = new InetSocketAddress(sourceIp, port);
                    state = STATE_PROTOCOL_DEST_PORT;
                }
                case STATE_PROTOCOL_DEST_PORT: {
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
                case STATE_SKIP_TO_END: {
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
