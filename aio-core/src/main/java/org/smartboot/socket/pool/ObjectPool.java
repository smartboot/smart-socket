package org.smartboot.socket.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对象池
 *
 * @author helyho
 * <p>
 * Vestful Framework.
 * WebSite: https://github.com/helyho/Vestful
 * Licence: Apache v2 License
 */
public abstract class ObjectPool<K, T> {
    private static final Logger LOGGER = LogManager.getLogger(ObjectPool.class);
    private Map<K, ArrayBlockingQueue<T>> objects = new ConcurrentHashMap<>();
    private int poolSize = 100;

    public ObjectPool() {
        this(100);
    }

    public ObjectPool(int poolSize) {
        if (poolSize > 0)
            this.poolSize = poolSize;
    }

    public T acquire(K key) {
        ArrayBlockingQueue<T> list = objects.get(key);
        if (list == null) {
            synchronized (ObjectPool.class) {
                if (list == null) {
                    list = new ArrayBlockingQueue<T>(poolSize);
                    objects.put(key, list);
                    return init(key);
                }
            }
            LOGGER.info("new Object for {}", key);
            return acquire(key);
        }
        T t = list.poll();
        if (t == null) {
            LOGGER.warn("pool not enough,key:{}", key);
            return init(key);
        } else {
            LOGGER.info("acquire bytebuffer success ,key:{},size:{}", key, list.size());
            return t;
        }
//        return t != null ? t : init();
    }


    public void release(K key, T t) {
        ArrayBlockingQueue<T> list = objects.get(key);
        if (list == null) {
            LOGGER.info("no pool for {}", key);
            return;
        }
        if (!list.offer(t)) {
            LOGGER.info("cache key:{} into pool fail", key);
        } else {
            LOGGER.info("cache key:{} into pool success,size:{}", key, list.size());
        }
    }

    public abstract T init(K key);
}

