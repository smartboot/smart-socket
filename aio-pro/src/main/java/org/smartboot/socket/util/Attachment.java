package org.smartboot.socket.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/1
 */
public class Attachment {
    private Map<String, AttachKey> attachKeyMap = new ConcurrentHashMap<>();

    public <T> void put(AttachKey<T> key, T t) {
        key.setValue(t);
        attachKeyMap.put(key.getKey(), key);
    }


    public <T> T get(AttachKey<T> key) {
        return (T) attachKeyMap.get(key.getKey()).getValue();
    }

    public <T> void remove(AttachKey<T> key) {
        attachKeyMap.remove(key.getKey());
    }
}
