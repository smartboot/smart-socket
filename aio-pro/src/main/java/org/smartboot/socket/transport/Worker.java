package org.smartboot.socket.transport;

import org.smartboot.socket.DecoderException;
import org.smartboot.socket.NetMonitor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPage;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Worker implements Runnable {
    private final static int MAX_READ_TIMES = 16;
    private static final Runnable SELECTOR_CHANNEL = () -> {
    };
    private static final Runnable SHUTDOWN_CHANNEL = () -> {
    };
    /**
     * 当前Worker绑定的Selector
     */
    private final Selector selector;
    /**
     * write 内存池
     */
    private BufferPagePool writeBufferPool = null;
    /**
     * read 内存池
     */
    private BufferPage readBufferPage = null;
    private final BlockingQueue<Runnable> requestQueue = new ArrayBlockingQueue<>(256);

    /**
     * 待注册的事件
     */
    private final ConcurrentLinkedQueue<Consumer<Selector>> registers = new ConcurrentLinkedQueue<>();

    private VirtualBuffer standbyBuffer;
    private final ExecutorService executorService;

    public Worker(BufferPagePool writeBufferPool, int threadNum) throws IOException {
        this(writeBufferPool.allocateBufferPage(), writeBufferPool, threadNum);
    }

    public Worker(BufferPage readBufferPage, BufferPagePool writeBufferPool, int threadNum) throws IOException {
        this.readBufferPage = readBufferPage;
        this.writeBufferPool = writeBufferPool;
        this.selector = Selector.open();
        try {
            this.requestQueue.put(SELECTOR_CHANNEL);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //启动worker线程组
        executorService = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            int i = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "smart-socket:udp-" + Worker.this.hashCode() + "-" + (++i));
            }
        });
        for (int i = 0; i < threadNum; i++) {
            executorService.execute(this);
        }
    }

    /**
     * 注册事件
     */
    void addRegister(Consumer<Selector> register) {
        registers.offer(register);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = requestQueue.take();
                //服务终止
                if (runnable == SHUTDOWN_CHANNEL) {
                    requestQueue.put(SHUTDOWN_CHANNEL);
                    selector.wakeup();
                    break;
                } else if (runnable == SELECTOR_CHANNEL) {
                    try {
                        doSelector();
                    } finally {
                        requestQueue.put(SELECTOR_CHANNEL);
                    }
                } else {
                    runnable.run();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSelector() throws IOException {
        Consumer<Selector> register;
        while ((register = registers.poll()) != null) {
            register.accept(selector);
        }
        Set<SelectionKey> keySet = selector.selectedKeys();
        if (keySet.isEmpty()) {
            selector.select();
        }
        Iterator<SelectionKey> keyIterator = keySet.iterator();
        // 执行本次已触发待处理的事件
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            UdpChannel udpChannel = (UdpChannel) key.attachment();
            if (!key.isValid()) {
                keyIterator.remove();
                udpChannel.close();
                continue;
            }
            if (key.isWritable()) {
                udpChannel.doWrite();
            }
            if (key.isReadable() && !doRead(udpChannel)) {
                break;
            }
            keyIterator.remove();
        }
    }

    private boolean doRead(UdpChannel channel) throws IOException {
        int count = MAX_READ_TIMES;
        IoServerConfig config = channel.config;
        while (count-- > 0) {
            if (standbyBuffer == null) {
                standbyBuffer = readBufferPage.allocate(config.getReadBufferSize());
            }
            ByteBuffer buffer = standbyBuffer.buffer();
            SocketAddress remote = channel.getChannel().receive(buffer);
            if (remote == null) {
                buffer.clear();
                return true;
            }
            VirtualBuffer readyBuffer = standbyBuffer;
            standbyBuffer = readBufferPage.allocate(config.getReadBufferSize());
            buffer.flip();
            Runnable runnable = () -> {
                //解码
                UdpAioSession session = new UdpAioSession(channel, remote, writeBufferPool.allocateBufferPage());
                try {
                    NetMonitor netMonitor = config.getMonitor();
                    if (netMonitor != null) {
                        netMonitor.beforeRead(session);
                        netMonitor.afterRead(session, buffer.remaining());
                    }
                    do {
                        Object request = config.getProtocol().decode(buffer, session);
                        //理论上每个UDP包都是一个完整的消息
                        if (request == null) {
                            config.getProcessor().stateEvent(session, StateMachineEnum.DECODE_EXCEPTION, new DecoderException("decode result is null, buffer size: " + buffer.remaining()));
                            break;
                        } else {
                            config.getProcessor().process(session, request);
                        }
                    } while (buffer.hasRemaining());
                } catch (Throwable e) {
                    e.printStackTrace();
                    config.getProcessor().stateEvent(session, StateMachineEnum.DECODE_EXCEPTION, e);
                } finally {
                    session.writeBuffer().flush();
                    readyBuffer.clean();
                }
            };
            if (!requestQueue.offer(runnable)) {
                return false;
            }
        }
        return true;
    }


    void shutdown() {
        try {
            requestQueue.put(SHUTDOWN_CHANNEL);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        selector.wakeup();
        executorService.shutdown();
        try {
            selector.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}