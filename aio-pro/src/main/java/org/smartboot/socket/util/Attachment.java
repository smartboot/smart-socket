package org.smartboot.socket.util;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/1
 */
public class Attachment {

    private Object[] values = new Object[8];

    public <T> void put(AttachKey<T> key, T t) {
        int index = key.getKeyIndex();
        if (index > values.length) {
            Object[] old = values;
            int i = 1;
            do {
                i <<= 1;
            } while (i < index);
            values = new Object[i];
            System.arraycopy(old, 0, values, 0, old.length);
        }
        values[index] = t;
    }


    public <T> T get(AttachKey<T> key) {
        return (T) values[key.getKeyIndex()];
    }

    public <T> void remove(AttachKey<T> key) {
        values[key.getKeyIndex()] = null;
    }
}
