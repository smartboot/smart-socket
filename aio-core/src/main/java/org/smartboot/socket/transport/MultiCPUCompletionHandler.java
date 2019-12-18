/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: ReadCompletionHandler.java
 * Date: 2017-11-25
 * Author: sandao
 */

package org.smartboot.socket.transport;

import java.util.concurrent.ExecutorService;

/**
 * 多核CPU读写事件回调处理类。
 * 多核CPU下需要将IO和业务处理进行资源隔离，以降低业务性能拖累IO能力。
 *
 * @author 三刀
 * @version V1.0.0
 */
final class MultiCPUCompletionHandler<T> extends ReadCompletionHandler<T> {

    private ExecutorService executorService;

    MultiCPUCompletionHandler(final ExecutorService executorService) {
        this.executorService = executorService;
    }


    @Override
    public void completed(final Integer result, final TcpAioSession<T> aioSession) {
        executorService.execute(() -> super.completed(result, aioSession));
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
    }
}