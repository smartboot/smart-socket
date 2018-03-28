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
    private int poolSize = 64;

    public ObjectPool() {
        this(0);
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
                    for (int i = 0; i < poolSize - 1; i++) {
                        list.offer(init(key));
                    }
                    return init(key);
                }
            }
            return acquire(key);
        }
        T t = list.poll();
        if (t == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("pool not enough,key:{}", key);
            }
            return emptyPool(key);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("acquire bytebuffer success ,key:{},size:{}", key, list.size());
            }
            return t;
        }
//        return t != null ? t : init();
    }


    public void release(K key, T t) {
        ArrayBlockingQueue<T> list = objects.get(key);
        if (list == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no pool for {}", key);
            }
            return;
        }
        boolean suc = list.offer(t);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("release key:{} {}", key, suc ? "success" : "fail");
        }
    }

    public abstract T init(K key);

    public abstract T emptyPool(K key);
}

