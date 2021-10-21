/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RateLimiterPlugin.java
 * Date: 2021-08-14
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.plugins;

import org.smartboot.socket.channels.AsynchronousSocketChannelProxy;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络流量控制插件
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/29
 */
public class RateLimiterPlugin<T> extends AbstractPlugin<T> {

    /**
     * read 流控阈值
     */
    private final int readRateLimiter;
    /**
     * write 流控阈值
     */
    private final int writeRateLimiter;
    /**
     * 流控功能是否启用
     */
    private final boolean enabled;
    private final int bufferTime = 10;
    private ScheduledExecutorService executorService;

    public RateLimiterPlugin(int readRateLimiter, int writeRateLimiter) {
        this.readRateLimiter = readRateLimiter;
        this.writeRateLimiter = writeRateLimiter;
        this.enabled = readRateLimiter > 0 && writeRateLimiter > 0;
        if (enabled) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }
    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return enabled ? new RateLimiterChannel(channel, readRateLimiter, writeRateLimiter) : channel;
    }

    /**
     * 具备流控能力的通道
     */
    class RateLimiterChannel extends AsynchronousSocketChannelProxy {
        private final int readRateLimiter;
        private final int writeRateLimiter;
        /**
         * 上一次read流控窗口临界点
         */
        private long latestReadTime;

        /**
         * 流控窗口期输入字节数
         */
        private int readSize;
        /**
         * 上一次write流控窗口临界点
         */
        private long latestWriteTime;
        /**
         * 流控窗口期输出字节数
         */
        private int writeCount;

        public RateLimiterChannel(AsynchronousSocketChannel asynchronousSocketChannel, int readRateLimiter, int writeRateLimiter) {
            super(asynchronousSocketChannel);
            this.readRateLimiter = readRateLimiter;
            this.writeRateLimiter = writeRateLimiter;
        }

        @Override
        public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (dst.remaining() == 0 || readRateLimiter <= 0) {
                super.read(dst, timeout, unit, attachment, handler);
                return;
            }
            int availReadSize = 0;
            long remainTime = 1000 + latestReadTime - System.currentTimeMillis();
            //新的流控窗口
            if (remainTime <= bufferTime) {
                readSize = 0;
                latestReadTime = System.currentTimeMillis();
            }
            availReadSize = Math.min(readRateLimiter - readSize, dst.remaining());
            //触发流控
            if (availReadSize <= 0) {
//                System.out.println("读流控 " + availReadSize);
                executorService.schedule(() -> RateLimiterChannel.this.read(dst, timeout, unit, attachment, handler), remainTime, TimeUnit.MILLISECONDS);
                return;
            }

            int limit = dst.limit();
            //限制limit,防止流控溢出
            dst.limit(dst.position() + availReadSize);
            super.read(dst, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
                @Override
                public void completed(Integer result, A attachment) {
                    if (result > 0) {
                        //是否开启新的流控窗口
                        if (System.currentTimeMillis() - latestReadTime > 1000) {
                            readSize = 0;
                            latestReadTime = System.currentTimeMillis();
                        } else {
                            readSize += result;
                        }
                    }
                    //重置limit
                    dst.limit(limit);
                    handler.completed(result, attachment);
                }

                @Override
                public void failed(Throwable exc, A attachment) {
                    handler.failed(exc, attachment);
                }
            });
        }

        @Override
        public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (src.remaining() == 0 || writeRateLimiter <= 0) {
                super.write(src, timeout, unit, attachment, handler);
                return;
            }
            int availWriteSize = 0;
            long remainTime = 1000 + latestWriteTime - System.currentTimeMillis();
            //新的流控窗口
            if (remainTime <= bufferTime) {
                writeCount = 0;
                latestWriteTime = System.currentTimeMillis();
            }
            availWriteSize = Math.min(writeRateLimiter - writeCount, src.remaining());
            //触发流控
            if (availWriteSize <= 0) {
//                System.out.println("写流控 " + availWriteSize);
                executorService.schedule(() -> RateLimiterChannel.this.write(src, timeout, unit, attachment, handler), remainTime, TimeUnit.MILLISECONDS);
                return;
            }

            int limit = src.limit();
            //限制limit,防止流控溢出
            src.limit(src.position() + availWriteSize);
            super.write(src, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
                @Override
                public void completed(Integer result, A attachment) {
                    if (result > 0) {
                        //是否开启新的流控窗口
                        if (System.currentTimeMillis() - latestWriteTime > 1000) {
                            writeCount = 0;
                            latestWriteTime = System.currentTimeMillis();
                        } else {
                            writeCount += result;
                        }
                    }
                    //重置limit
                    src.limit(limit);
                    handler.completed(result, attachment);
                }

                @Override
                public void failed(Throwable exc, A attachment) {
                    handler.failed(exc, attachment);
                }
            });
        }
    }
}
