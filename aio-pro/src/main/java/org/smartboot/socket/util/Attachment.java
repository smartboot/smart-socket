/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Attachment.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/1
 */
public class Attachment {

    /**
     * 附件值
     */
    private final Map<AttachKey, Object> values = new HashMap<>();

    /**
     * 存储附件
     *
     * @param key   附件Key
     * @param value 附件值
     * @param <T>   附件值
     */
    public <T> void put(AttachKey<T> key, T value) {
        values.put(key, value);
    }

    /**
     * 获取附件对象
     *
     * @param key 附件Key
     * @param <T> 附件值
     * @return 附件值
     */
    public <T> T get(AttachKey<T> key) {
        return (T) values.get(key);
    }

    /**
     * 移除附件
     *
     * @param key 附件Key
     * @param <T> 附件值
     */
    public <T> void remove(AttachKey<T> key) {
        values.remove(key);
    }
}
