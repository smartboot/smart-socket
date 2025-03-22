/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: EnhanceAsynchronousChannelGroup.java
 * Date: 2021-07-29
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version V1.0 , 2020/5/25
 */
class EnhanceAsynchronousChannelGroup extends AsynchronousChannelGroup {
    /**
     * 递归回调次数上限
     */
    public static final int MAX_INVOKER = 8;

    /**
     * 读回调处理线程池,可用于业务处理
     */
    private final ExecutorService readExecutorService;
    /**
     * 写回调线程池
     */
    private final ExecutorService commonExecutorService;
    /**
     * write工作组
     */
    final Worker commonWorker;
    /**
     * read工作组
     */
    private final Worker[] readWorkers;
    final Worker writeWorker;
    /**
     * 线程池分配索引
     */
    private final AtomicInteger readIndex = new AtomicInteger(0);

    /**
     * group运行状态
     */
    boolean running = true;

    /**
     * Initialize a new instance of this class.
     *
     * @param provider The asynchronous channel provider for this group
     */
    protected EnhanceAsynchronousChannelGroup(AsynchronousChannelProvider provider, ExecutorService readExecutorService, int threadNum) throws IOException {
        super(provider);
        //init threadPool for read
        this.readExecutorService = readExecutorService;
        this.readWorkers = new Worker[threadNum];
        for (int i = 0; i < threadNum; i++) {
            readWorkers[i] = new Worker(Selector.open(), selectionKey -> {
                EnhanceAsynchronousServerChannel asynchronousSocketChannel = (EnhanceAsynchronousServerChannel) selectionKey.attachment();
                asynchronousSocketChannel.doRead(true);
            });
            this.readExecutorService.execute(readWorkers[i]);
        }

        //init threadPool for write and connect
        writeWorker = new Worker(Selector.open(), selectionKey -> {
            EnhanceAsynchronousServerChannel asynchronousSocketChannel = (EnhanceAsynchronousServerChannel) selectionKey.attachment();
            //直接调用interestOps的效果比 removeOps(selectionKey, SelectionKey.OP_WRITE) 更好
            if (running) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            }
            while (asynchronousSocketChannel.doWrite()) ;
        });
        commonWorker = new Worker(Selector.open(), selectionKey -> {
            if (selectionKey.isAcceptable()) {
                EnhanceAsynchronousServerSocketChannel serverSocketChannel = (EnhanceAsynchronousServerSocketChannel) selectionKey.attachment();
                serverSocketChannel.doAccept();
            } else if (selectionKey.isConnectable()) {
                Runnable runnable = (Runnable) selectionKey.attachment();
                runnable.run();
            } else if (selectionKey.isReadable()) {
                //仅同步read会用到此线程资源
                EnhanceAsynchronousServerChannel asynchronousSocketChannel = (EnhanceAsynchronousServerChannel) selectionKey.attachment();
                removeOps(selectionKey, SelectionKey.OP_READ);
                asynchronousSocketChannel.doRead(true);
            } else {
                throw new IllegalStateException("unexpect callback,key valid:" + selectionKey.isValid() + " ,interestOps:" + selectionKey.interestOps());
            }
        });

        commonExecutorService = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "smart-socket:common"));
        commonExecutorService.execute(writeWorker);
        commonExecutorService.execute(commonWorker);
    }

    /**
     * 移除关注事件
     *
     * @param selectionKey 待操作的selectionKey
     * @param opt          移除的事件
     */
    public static void removeOps(SelectionKey selectionKey, int opt) {
        if (selectionKey.isValid() && (selectionKey.interestOps() & opt) != 0) {
            selectionKey.interestOps(selectionKey.interestOps() & ~opt);
        }
    }

    public Worker getReadWorker() {
        return readWorkers[(readIndex.getAndIncrement() & Integer.MAX_VALUE) % readWorkers.length];
    }

    @Override
    public boolean isShutdown() {
        return readExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return readExecutorService.isTerminated();
    }

    @Override
    public void shutdown() {
        running = false;
        commonWorker.workerThread.interrupt();
        writeWorker.workerThread.interrupt();
        for (Worker worker : readWorkers) {
            worker.workerThread.interrupt();
        }
        readExecutorService.shutdown();
        commonExecutorService.shutdown();
    }

    @Override
    public void shutdownNow() {
        shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return readExecutorService.awaitTermination(timeout, unit);
    }

    public static void interestOps(Worker worker, SelectionKey selectionKey, int opt) {
        if ((selectionKey.interestOps() & opt) != 0) {
            return;
        }
        selectionKey.interestOps(selectionKey.interestOps() | opt);
        //Worker线程无需wakeup
        if (worker.workerThread != Thread.currentThread()) {
            selectionKey.selector().wakeup();
        }
    }

    class Worker implements Runnable {
        /**
         * 当前Worker绑定的Selector
         */
        final Selector selector;
        private final Consumer<SelectionKey> consumer;
        private final ConcurrentLinkedQueue<Consumer<Selector>> consumers = new ConcurrentLinkedQueue<>();
        private Thread workerThread;

        Worker(Selector selector, Consumer<SelectionKey> consumer) {
            this.selector = selector;
            this.consumer = consumer;
        }

        /**
         * 注册事件
         */
        final void addRegister(Consumer<Selector> register) {
            consumers.offer(register);
            selector.wakeup();
        }

        @Override
        public final void run() {
            workerThread = Thread.currentThread();
            // 优先获取SelectionKey,若无关注事件触发则阻塞在selector.select(),减少select被调用次数
            Set<SelectionKey> keySet = selector.selectedKeys();
            try {
                while (running) {
                    Consumer<Selector> selectorConsumer;
                    while ((selectorConsumer = consumers.poll()) != null) {
                        selectorConsumer.accept(selector);
                    }
                    selector.select();

                    // 执行本次已触发待处理的事件
                    for (SelectionKey key : keySet) {
                        consumer.accept(key);
                    }
                    keySet.clear();
                }
                selector.keys().forEach(key -> {
                    try {
                        consumer.accept(key);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
