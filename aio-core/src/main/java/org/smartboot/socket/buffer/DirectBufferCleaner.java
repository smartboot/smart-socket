package org.smartboot.socket.buffer;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@SuppressWarnings("restriction")
class DirectBufferCleaner {

    // Java 8及以下：使用 DirectBuffer 的 cleaner() 方法
    private static Consumer<ByteBuffer> cleaner = buffer -> ((DirectBuffer) buffer).cleaner().clean();
    /**
     * 标识当前JDK版本是否支持直接缓冲区的清理操作。
     * 由于JDK 9及以上版本对sun.nio.ch.DirectBuffer的访问有限制，
     * 因此在高版本JDK中默认不支持直接缓冲区模式。
     */
    private static boolean directSupported = true;

    static {

        if (getJavaMajorVersion() >= 9) {
            try {
                // 通过反射获取 Unsafe 实例
                Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
                // 获取 invokeCleaner(ByteBuffer) 方法
                Method invokeCleaner = Unsafe.class.getMethod("invokeCleaner", ByteBuffer.class);
                cleaner = buffer -> {
                    try {
                        // Java 9及以上：通过 Unsafe.invokeCleaner(ByteBuffer) 进行清理
                        invokeCleaner.invoke(unsafe, buffer);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to clean direct buffer", e);
                    }
                };
            } catch (Throwable e) {
                directSupported = false;
                // 如果获取失败，则后续调用将退化为直接使用 DirectBuffer 的 cleaner
                e.printStackTrace();
            }
        }
    }

    public static boolean isDirectSupported() {
        return directSupported;
    }

    /**
     * 清理 VirtualBuffer 所关联的直接内存
     *
     * @param buffer 包含直接 ByteBuffer 的包装类
     */
    public static void clean(ByteBuffer buffer) {
        cleaner.accept(buffer);
    }

    /**
     * 获取 Java 主版本号 如 "1.8" 返回 8，"9" 返回 9，"11" 返回 11
     */
    private static int getJavaMajorVersion() {
        String version = System.getProperty("java.specification.version");
        // 处理类似"1.8.0_301"和"9+"两种版本格式
        return version.startsWith("1.") ? Integer.parseInt(version.split("\\.")[1])
                : Integer.parseInt(version.split("\\.")[0]);
    }
}