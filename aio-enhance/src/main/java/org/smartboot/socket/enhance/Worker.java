/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Worker.java
 * Date: 2021-11-30
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.enhance;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

abstract class Worker implements Runnable {
    /**
     * 当前Worker绑定的Selector
     */
    protected final Selector selector;
    /**
     * 待注册的事件
     */
    protected ConcurrentLinkedQueue<Consumer<Selector>> registers;
    protected Thread workerThread;
    protected Set<SelectionKey> keySet;
    int invoker = 0;

    public Worker(Selector selector) {
        this(selector, new ConcurrentLinkedQueue<>());
    }

    public Worker(Selector selector, ConcurrentLinkedQueue<Consumer<Selector>> registers) {
        this.selector = selector;
        this.registers = registers;
        this.keySet = selector.selectedKeys();
    }

    public Thread getWorkerThread() {
        return workerThread;
    }

    /**
     * 注册事件
     */
    public final void addRegister(Consumer<Selector> register) {
        registers.offer(register);
        selector.wakeup();
    }
}