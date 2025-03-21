/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StringMutilClient.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.benchmark;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.extension.protocol.StringProtocol;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2018/11/23
 */
public class StringMutilClient {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        System.setProperty("smart-socket.session.writeChunkSize", "" + (1024 * 1024));

        BufferPagePool bufferPagePool = new BufferPagePool(1024 * 1024 * 32, 10, true);
        AbstractMessageProcessor processor = new AbstractMessageProcessor() {
            @Override
            public void process0(AioSession session, Object msg) {

            }

            @Override
            public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };
//        processor.addPlugin(new HeartPlugin(3000) {
//            @Override
//            public void sendHeartRequest(AioSession session) throws IOException {
//
//            }
//
//            @Override
//            public boolean isHeartMessage(AioSession session, Object msg) {
//                return true;
//            }
//        });
        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), processor);
        client.setBufferPagePool(bufferPagePool);
        client.setWriteBuffer(512, 20);
        AioSession session = client.start();
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        WriteBuffer outputStream = session.writeBuffer();
                        byte[] data = "smart-s1ocket".getBytes();
                        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);
                        buffer.putInt(data.length);
                        buffer.put(data);
                        byte[] a = buffer.array();
                        while (true) {

                            outputStream.write(a);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }

    }

}
