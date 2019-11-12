package org.smartboot.socket.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/2
 */
public final class AttachKey<T> {
    public static final int MAX_ATTACHE_COUNT = 128;
    private static final ConcurrentMap<String, AttachKey> NAMES = new ConcurrentHashMap<>();
    private static final AtomicInteger index = new AtomicInteger(0);
    private final String key;
    private final int keyIndex;

    private AttachKey(String key) {
        this.key = key;
        this.keyIndex = index.getAndIncrement();
        if (this.keyIndex < 0 || this.keyIndex >= MAX_ATTACHE_COUNT) {
            throw new RuntimeException("too many attach key");
        }
    }

    public static <T> AttachKey<T> valueOf(String name) {
        AttachKey<T> option = NAMES.get(name);
        if (option == null) {
            option = new AttachKey<T>(name);
            AttachKey<T> old = NAMES.putIfAbsent(name, option);
            if (old != null) {
                option = old;
            }
        }
        return option;
    }

    public String getKey() {
        return key;
    }


    int getKeyIndex() {
        return keyIndex;
    }
}
