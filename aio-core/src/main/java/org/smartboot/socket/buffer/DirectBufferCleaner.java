package org.smartboot.socket.buffer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

@SuppressWarnings("restriction")
public class DirectBufferCleaner {

  // 获取当前 JDK 的主版本号
  public static final int JAVA_MAJOR_VERSION = getJavaMajorVersion();

  // Java 9及以上使用的 Unsafe 实例和 invokeCleaner 方法
  private static final Unsafe UNSAFE;
  private static final Method INVOKE_CLEANER_METHOD;

  static {
    if (JAVA_MAJOR_VERSION >= 9) {
      Unsafe unsafe = null;
      Method invokeCleaner = null;
      try {
        // 通过反射获取 Unsafe 实例
        Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        unsafe = (Unsafe) theUnsafeField.get(null);
        // 获取 invokeCleaner(ByteBuffer) 方法
        invokeCleaner = Unsafe.class.getMethod("invokeCleaner", ByteBuffer.class);
      } catch (Exception e) {
        // 如果获取失败，则后续调用将退化为直接使用 DirectBuffer 的 cleaner
        e.printStackTrace();
      }
      UNSAFE = unsafe;
      INVOKE_CLEANER_METHOD = invokeCleaner;
    } else {
      UNSAFE = null;
      INVOKE_CLEANER_METHOD = null;
    }
  }

  /**
   * 清理 VirtualBuffer 所关联的直接内存
   * 
   * @param buffer 包含直接 ByteBuffer 的包装类
   */
  public static void clean(ByteBuffer buffer) {
    if (JAVA_MAJOR_VERSION >= 9 && UNSAFE != null && INVOKE_CLEANER_METHOD != null) {
      try {
        // Java 9及以上：通过 Unsafe.invokeCleaner(ByteBuffer) 进行清理
        INVOKE_CLEANER_METHOD.invoke(UNSAFE, buffer);
      } catch (Exception e) {
        throw new RuntimeException("Failed to clean direct buffer", e);
      }
    } else {
      // Java 8及以下：使用 DirectBuffer 的 cleaner() 方法
      ((DirectBuffer) buffer).cleaner().clean();
    }
  }

  /**
   * 获取 Java 主版本号 如 "1.8" 返回 8，"9" 返回 9，"11" 返回 11
   */
  private static int getJavaMajorVersion() {
    String version = System.getProperty("java.specification.version");
    return version.startsWith("1.") ? Integer.parseInt(version.split("\\.")[1])
        : Integer.parseInt(version.split("\\.")[0]);
  }
}