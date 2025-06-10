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
 * 增强型异步通道组实现类，继承自AsynchronousChannelGroup。
 * 该类通过多线程和Selector机制实现高性能的异步IO操作，主要功能包括：
 * 1. 管理读写线程池，实现IO事件的异步处理
 * 2. 维护多个Worker线程，分别处理读、写和通用事件
 * 3. 通过Selector机制实现事件的监听和分发
 * 4. 支持优雅关闭和资源回收
 * <p>
 * 该类是smart-socket框架的核心组件，采用了多线程模型来处理不同类型的IO事件：
 * - 读事件由多个Worker线程处理，实现负载均衡
 * - 写事件由单独的Worker线程处理，避免并发写入问题
 * - 通用事件（如accept、connect）由专门的Worker线程处理
 * <p>
 * 通过精细的线程分工和事件管理，实现了高性能、低延迟的网络IO处理能力。
 *
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
     * 忽略 SelectionKey.OP_ACCEPT，该情况不存在资源回收需求
     */
    private static final Consumer<SelectionKey> shutdownCallback = selectionKey -> {
        int interestOps = selectionKey.interestOps();
        selectionKey.cancel();
        if ((interestOps & SelectionKey.OP_CONNECT) > 0) {
            Runnable runnable = (Runnable) selectionKey.attachment();
            runnable.run();
        }
        if ((interestOps & SelectionKey.OP_READ) > 0) {
            //仅同步read会用到此线程资源
            EnhanceAsynchronousSocketChannel asynchronousSocketChannel = (EnhanceAsynchronousSocketChannel) selectionKey.attachment();
            removeOps(selectionKey, SelectionKey.OP_READ);
            asynchronousSocketChannel.doRead(true);
        }
        if ((interestOps & SelectionKey.OP_WRITE) > 0) {
            EnhanceAsynchronousSocketChannel asynchronousSocketChannel = (EnhanceAsynchronousSocketChannel) selectionKey.attachment();
            removeOps(selectionKey, SelectionKey.OP_WRITE);
            asynchronousSocketChannel.doWrite();
        }
    };

    /**
     * 初始化异步通道组实例。
     * 该构造函数完成以下初始化工作：
     * 1. 创建读事件处理线程池，包含多个Worker线程，每个Worker都有独立的Selector
     * 2. 创建写事件处理线程，使用单独的Worker处理写操作
     * 3. 创建通用事件处理线程，负责处理accept、connect等操作
     * 4. 所有Worker线程都采用Selector机制实现事件监听
     *
     * @param provider            异步通道提供者，用于创建异步通道
     * @param readExecutorService 读事件处理线程池
     * @param threadNum           读事件处理线程数量
     */
    protected EnhanceAsynchronousChannelGroup(AsynchronousChannelProvider provider, ExecutorService readExecutorService, int threadNum) throws IOException {
        super(provider);
        //init threadPool for read
        this.readExecutorService = readExecutorService;
        this.readWorkers = new Worker[threadNum];
        for (int i = 0; i < threadNum; i++) {
            readWorkers[i] = new Worker(Selector.open(), selectionKey -> {
                EnhanceAsynchronousSocketChannel asynchronousSocketChannel = (EnhanceAsynchronousSocketChannel) selectionKey.attachment();
                asynchronousSocketChannel.doRead(true);
            });
            this.readExecutorService.execute(readWorkers[i]);
        }

        //init threadPool for write and connect
        writeWorker = new Worker(Selector.open(), selectionKey -> {
            EnhanceAsynchronousSocketChannel asynchronousSocketChannel = (EnhanceAsynchronousSocketChannel) selectionKey.attachment();
            //直接调用interestOps的效果比 removeOps(selectionKey, SelectionKey.OP_WRITE) 更好
            try {
                if (selectionKey.isValid()) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                }
            } catch (Throwable e) {
                System.err.println("remove write ops error");
                e.printStackTrace();
            } finally {
                while (asynchronousSocketChannel.doWrite()) ;
            }
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
                EnhanceAsynchronousSocketChannel asynchronousSocketChannel = (EnhanceAsynchronousSocketChannel) selectionKey.attachment();
                removeOps(selectionKey, SelectionKey.OP_READ);
                asynchronousSocketChannel.doRead(true);
            } else {
                throw new IllegalStateException("unexpect callback,key valid:" + selectionKey.isValid() + " ,interestOps:" + selectionKey.interestOps());
            }
        });

        commonExecutorService = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "smart-socket:common-" + EnhanceAsynchronousChannelGroup.this.hashCode()));
        commonExecutorService.execute(writeWorker);
        commonExecutorService.execute(commonWorker);
    }

    /**
     * 移除指定SelectionKey的事件关注
     * 该方法用于取消对特定IO事件的监听，通常在完成某个IO操作后调用
     * 例如：完成读操作后，移除READ事件的关注
     *
     * @param selectionKey 待操作的selectionKey，代表一个Channel的注册信息
     * @param opt          需要移除的事件类型，如SelectionKey.OP_READ, SelectionKey.OP_WRITE等
     */
    public static void removeOps(SelectionKey selectionKey, int opt) {
        if (selectionKey.isValid() && (selectionKey.interestOps() & opt) != 0) {
            selectionKey.interestOps(selectionKey.interestOps() & ~opt);
        }
    }

    /**
     * 获取一个读事件处理Worker
     * 采用轮询方式分配Worker，确保负载均衡
     * 使用位运算优化取模运算，提高性能
     *
     * @return 返回一个可用的读事件处理Worker
     */
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

    /**
     * 优雅关闭异步通道组
     * 关闭步骤：
     * 1. 修改运行状态标志
     * 2. 中断所有Worker线程
     * 3. 关闭读写线程池
     * 该方法确保资源能够被正确释放
     */
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

    /**
     * 为指定的SelectionKey添加事件关注
     * 该方法用于注册新的IO事件监听，支持线程安全的事件注册
     * 特点：
     * 1. 避免重复注册相同事件
     * 2. 处理多线程并发注册场景
     * 3. 优化Selector的唤醒机制
     *
     * @param worker       当前的Worker实例
     * @param selectionKey 待操作的selectionKey
     * @param opt          需要添加的事件类型，如SelectionKey.OP_READ等
     */
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

    /**
     * Worker内部类，实现了Runnable接口，是异步通道组的核心工作单元。
     * 每个Worker都维护一个独立的Selector，负责特定类型事件(读/写/通用)的处理。
     * Worker的主要职责包括：
     * 1. 管理Selector生命周期
     * 2. 处理IO事件回调
     * 3. 维护事件注册队列
     * 4. 实现事件循环机制
     */
    class Worker implements Runnable {
        /**
         * 当前Worker绑定的Selector，用于IO事件的多路复用
         */
        final Selector selector;
        /**
         * 事件处理回调函数，针对不同类型的Worker（读/写/通用）有不同的实现
         */
        private final Consumer<SelectionKey> consumer;
        /**
         * 事件注册队列，用于存放待注册的Selector操作
         * 采用ConcurrentLinkedQueue确保线程安全
         */
        private final ConcurrentLinkedQueue<Consumer<Selector>> consumers = new ConcurrentLinkedQueue<>();
        /**
         * Worker线程引用，用于线程管理和中断操作
         */
        private Thread workerThread;

        Worker(Selector selector, Consumer<SelectionKey> consumer) {
            this.selector = selector;
            this.consumer = consumer;
        }

        /**
         * 注册新的Selector事件
         * 该方法支持异步注册事件，通过以下机制实现：
         * 1. 将注册事件添加到队列
         * 2. 唤醒Selector以处理新事件
         * 3. 确保线程安全的事件注册
         *
         * @param register Selector事件处理器
         */
        final void addRegister(Consumer<Selector> register) {
            consumers.offer(register);
            selector.wakeup();
        }


        EnhanceAsynchronousChannelGroup group() {
            return EnhanceAsynchronousChannelGroup.this;
        }

        /**
         * Worker线程的主循环方法
         * 实现事件循环机制，主要职责：
         * 1. 处理待注册的事件
         * 2. 通过Selector监听IO事件
         * 3. 分发和处理已就绪的事件
         * 4. 确保资源的正确释放
         */
        @Override
        public final void run() {
            workerThread = Thread.currentThread();
            // 优先获取SelectionKey,若无关注事件触发则阻塞在selector.select(),减少select被调用次数
            Set<SelectionKey> keySet = selector.selectedKeys();
            try {
                while (running) {
                    // 处理待注册的事件
                    Consumer<Selector> selectorConsumer;
                    while ((selectorConsumer = consumers.poll()) != null) {
                        selectorConsumer.accept(selector);
                    }
                    // 阻塞等待IO事件
                    selector.select();
                    // 处理已就绪的IO事件
                    for (SelectionKey key : keySet) {
                        consumer.accept(key);
                    }
                    keySet.clear();
                }
                // 关闭前处理剩余的事件
                selector.keys().stream().filter(SelectionKey::isValid).forEach(key -> {
                    try {
                        shutdownCallback.accept(key);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
            } catch (Throwable e) {
                if (running) {
                    System.err.println("worker thread error");
                }
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
