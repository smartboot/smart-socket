package org.smartboot.socket.transport;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

class Util {
    private static final int TEMP_BUF_POOL_SIZE;
    private static final long MAX_CACHED_BUFFER_SIZE;
    private static ThreadLocal<Util.BufferCache> bufferCache;
    private static Unsafe unsafe;
    private static int pageSize;
    private static volatile Constructor<?> directByteBufferConstructor;
    private static volatile Constructor<?> directByteBufferRConstructor;
    private static volatile String bugLevel;

    static {
        TEMP_BUF_POOL_SIZE = 1024;//iovMax();
        MAX_CACHED_BUFFER_SIZE = getMaxCachedBufferSize();
        bufferCache = new ThreadLocal<Util.BufferCache>() {
            protected Util.BufferCache initialValue() {
                return new Util.BufferCache();
            }
        };
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        pageSize = -1;
        directByteBufferConstructor = null;
        directByteBufferRConstructor = null;
        bugLevel = null;
    }

    public Util() {
    }

    private static long getMaxCachedBufferSize() {
        String var0 = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("jdk.nio.maxCachedBufferSize");
            }
        });
        if (var0 != null) {
            try {
                long var1 = Long.parseLong(var0);
                if (var1 >= 0L) {
                    return var1;
                }
            } catch (NumberFormatException var3) {
                ;
            }
        }

        return 9223372036854775807L;
    }

    private static boolean isBufferTooLarge(int var0) {
        return (long) var0 > MAX_CACHED_BUFFER_SIZE;
    }

    private static boolean isBufferTooLarge(ByteBuffer var0) {
        return isBufferTooLarge(var0.capacity());
    }

    public static ByteBuffer getTemporaryDirectBuffer(int var0) {
        if (isBufferTooLarge(var0)) {
            return ByteBuffer.allocateDirect(var0);
        } else {
            Util.BufferCache var1 = (Util.BufferCache) bufferCache.get();
            ByteBuffer var2 = var1.get(var0);
            if (var2 != null) {
                return var2;
            } else {
                if (!var1.isEmpty()) {
                    var2 = var1.removeFirst();
                    free(var2);
                }

                return ByteBuffer.allocateDirect(var0);
            }
        }
    }

    public static void releaseTemporaryDirectBuffer(ByteBuffer var0) {
        offerFirstTemporaryDirectBuffer(var0);
    }

    static void offerFirstTemporaryDirectBuffer(ByteBuffer var0) {
        if (isBufferTooLarge(var0)) {
            free(var0);
        } else {
            assert var0 != null;

            Util.BufferCache var1 = (Util.BufferCache) bufferCache.get();
            if (!var1.offerFirst(var0)) {
                free(var0);
            }

        }
    }

    static void offerLastTemporaryDirectBuffer(ByteBuffer var0) {
        if (isBufferTooLarge(var0)) {
            free(var0);
        } else {
            assert var0 != null;

            Util.BufferCache var1 = (Util.BufferCache) bufferCache.get();
            if (!var1.offerLast(var0)) {
                free(var0);
            }

        }
    }

    private static void free(ByteBuffer var0) {
        ((DirectBuffer) var0).cleaner().clean();
    }

    static ByteBuffer[] subsequence(ByteBuffer[] var0, int var1, int var2) {
        if (var1 == 0 && var2 == var0.length) {
            return var0;
        } else {
            int var3 = var2;
            ByteBuffer[] var4 = new ByteBuffer[var2];

            for (int var5 = 0; var5 < var3; ++var5) {
                var4[var5] = var0[var1 + var5];
            }

            return var4;
        }
    }

    static <E> Set<E> ungrowableSet(final Set<E> var0) {
        return new Set<E>() {
            public int size() {
                return var0.size();
            }

            public boolean isEmpty() {
                return var0.isEmpty();
            }

            public boolean contains(Object var1) {
                return var0.contains(var1);
            }

            public Object[] toArray() {
                return var0.toArray();
            }

            public <T> T[] toArray(T[] var1) {
                return var0.toArray(var1);
            }

            public String toString() {
                return var0.toString();
            }

            public Iterator<E> iterator() {
                return var0.iterator();
            }

            public boolean equals(Object var1) {
                return var0.equals(var1);
            }

            public int hashCode() {
                return var0.hashCode();
            }

            public void clear() {
                var0.clear();
            }

            public boolean remove(Object var1) {
                return var0.remove(var1);
            }

            public boolean containsAll(Collection<?> var1) {
                return var0.containsAll(var1);
            }

            public boolean removeAll(Collection<?> var1) {
                return var0.removeAll(var1);
            }

            public boolean retainAll(Collection<?> var1) {
                return var0.retainAll(var1);
            }

            public boolean add(E var1) {
                throw new UnsupportedOperationException();
            }

            public boolean addAll(Collection<? extends E> var1) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static byte _get(long var0) {
        return unsafe.getByte(var0);
    }

    private static void _put(long var0, byte var2) {
        unsafe.putByte(var0, var2);
    }

    static void erase(ByteBuffer var0) {
        unsafe.setMemory(((DirectBuffer) var0).address(), (long) var0.capacity(), (byte) 0);
    }

    static Unsafe unsafe() {
        return unsafe;
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe().pageSize();
        }

        return pageSize;
    }

    private static void initDBBConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Class var1 = Class.forName("java.nio.DirectByteBuffer");
                    Constructor var2 = var1.getDeclaredConstructor(Integer.TYPE, Long.TYPE, FileDescriptor.class, Runnable.class);
                    var2.setAccessible(true);
                    Util.directByteBufferConstructor = var2;
                    return null;
                } catch (NoSuchMethodException | IllegalArgumentException | ClassCastException | ClassNotFoundException var3) {
                    throw new RuntimeException(var3);
                }
            }
        });
    }

    static MappedByteBuffer newMappedByteBuffer(int var0, long var1, FileDescriptor var3, Runnable var4) {
        if (directByteBufferConstructor == null) {
            initDBBConstructor();
        }

        try {
            MappedByteBuffer var5 = (MappedByteBuffer) directByteBufferConstructor.newInstance(new Integer(var0), new Long(var1), var3, var4);
            return var5;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException var7) {
            throw new RuntimeException(var7);
        }
    }

    private static void initDBBRConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Class var1 = Class.forName("java.nio.DirectByteBufferR");
                    Constructor var2 = var1.getDeclaredConstructor(Integer.TYPE, Long.TYPE, FileDescriptor.class, Runnable.class);
                    var2.setAccessible(true);
                    Util.directByteBufferRConstructor = var2;
                    return null;
                } catch (NoSuchMethodException | IllegalArgumentException | ClassCastException | ClassNotFoundException var3) {
                    throw new RuntimeException(var3);
                }
            }
        });
    }

    static MappedByteBuffer newMappedByteBufferR(int var0, long var1, FileDescriptor var3, Runnable var4) {
        if (directByteBufferRConstructor == null) {
            initDBBRConstructor();
        }

        try {
            MappedByteBuffer var5 = (MappedByteBuffer) directByteBufferRConstructor.newInstance(new Integer(var0), new Long(var1), var3, var4);
            return var5;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException var7) {
            throw new RuntimeException(var7);
        }
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        sun.nio.ch.Util util = new sun.nio.ch.Util();
        Field field = util.getClass().getDeclaredField("TEMP_BUF_POOL_SIZE");
        field.setAccessible(true);
        System.out.println(field.get(util));
    }

    private static class BufferCache {
        private ByteBuffer[] buffers;
        private int count;
        private int start;

        BufferCache() {
            this.buffers = new ByteBuffer[Util.TEMP_BUF_POOL_SIZE];
        }

        private int next(int var1) {
            return (var1 + 1) % Util.TEMP_BUF_POOL_SIZE;
        }

        ByteBuffer get(int var1) {
            assert !Util.isBufferTooLarge(var1);

            if (this.count == 0) {
                return null;
            } else {
                ByteBuffer[] var2 = this.buffers;
                ByteBuffer var3 = var2[this.start];
                if (var3.capacity() < var1) {
                    var3 = null;
                    int var4 = this.start;

                    while ((var4 = this.next(var4)) != this.start) {
                        ByteBuffer var5 = var2[var4];
                        if (var5 == null) {
                            break;
                        }

                        if (var5.capacity() >= var1) {
                            var3 = var5;
                            break;
                        }
                    }

                    if (var3 == null) {
                        return null;
                    }

                    var2[var4] = var2[this.start];
                }

                var2[this.start] = null;
                this.start = this.next(this.start);
                --this.count;
                var3.rewind();
                var3.limit(var1);
                return var3;
            }
        }

        boolean offerFirst(ByteBuffer var1) {
            assert !Util.isBufferTooLarge(var1);

            if (this.count >= Util.TEMP_BUF_POOL_SIZE) {
                return false;
            } else {
                this.start = (this.start + Util.TEMP_BUF_POOL_SIZE - 1) % Util.TEMP_BUF_POOL_SIZE;
                this.buffers[this.start] = var1;
                ++this.count;
                return true;
            }
        }

        boolean offerLast(ByteBuffer var1) {
            assert !Util.isBufferTooLarge(var1);

            if (this.count >= Util.TEMP_BUF_POOL_SIZE) {
                return false;
            } else {
                int var2 = (this.start + this.count) % Util.TEMP_BUF_POOL_SIZE;
                this.buffers[var2] = var1;
                ++this.count;
                return true;
            }
        }

        boolean isEmpty() {
            return this.count == 0;
        }

        ByteBuffer removeFirst() {
            assert this.count > 0;

            ByteBuffer var1 = this.buffers[this.start];
            this.buffers[this.start] = null;
            this.start = this.next(this.start);
            --this.count;
            return var1;
        }
    }
}
