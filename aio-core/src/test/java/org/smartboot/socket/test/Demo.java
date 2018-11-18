package org.smartboot.socket.test;

import java.nio.ByteBuffer;

public class Demo {
    public static void main(String[] args) {
        byte[] data = "smart-socket".getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(15);
        printBuffer("初始", buffer);
        buffer.put(data);
        printBuffer("写数据", buffer);
        buffer.flip();
        printBuffer("执行flip", buffer);
        byte[] read = new byte[buffer.remaining()];
        buffer.get(read);
        printBuffer("读数据", buffer);
        System.out.println(new String(read));
    }

    private static void printBuffer(String title, ByteBuffer buffer) {
        System.out.println(title + " position:" + buffer.position() + " ,limit:" + buffer.limit() + " ,capacity:" + buffer.capacity());
    }
}