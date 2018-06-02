package org.smartboot.socket.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/2
 */
public class AttachKey<T> {
    private static final ConcurrentMap<String, AttachKey> names = new ConcurrentHashMap<>();
    private String key;
    private T value;

    private AttachKey(String key) {
        this.key = key;
    }

    public static <T> AttachKey<T> valueOf(String name) {
        AttachKey<T> option = names.get(name);
        if (option == null) {
            option = new AttachKey<T>(name);
            AttachKey<T> old = names.putIfAbsent(name, option);
            if (old != null) {
                option = old;
            }
        }
        return option;
    }

    public String getKey() {
        return key;
    }

    T getValue() {
        return value;
    }

    void setValue(T value) {
        this.value = value;
    }
}
