/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: GroupMessageProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.processor;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.extension.group.GroupIo;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/9
 */
abstract class GroupMessageProcessor<T> implements MessageProcessor<T>, GroupIo<T> {

    private Map<String, GroupUnit> sessionGroup = new ConcurrentHashMap<>();

    /**
     * 将AioSession加入群组group
     *
     * @param group
     * @param session
     */
    @Override
    public final synchronized void join(String group, AioSession<T> session) {
        GroupUnit groupUnit = sessionGroup.get(group);
        if (groupUnit == null) {
            groupUnit = new GroupUnit();
            sessionGroup.put(group, groupUnit);
        }
        groupUnit.groupList.add(session);
    }

    @Override
    public final synchronized void remove(String group, AioSession<T> session) {
        GroupUnit groupUnit = sessionGroup.get(group);
        if (groupUnit == null) {
            return;
        }
        groupUnit.groupList.remove(session);
        if (groupUnit.groupList.isEmpty()) {
            sessionGroup.remove(group);
        }
    }

    @Override
    public final void remove(AioSession<T> session) {
        for (String group : sessionGroup.keySet()) {
            remove(group, session);
        }
    }

    @Override
    public void writeToGroup(String group, byte[] t) {
        GroupUnit groupUnit = sessionGroup.get(group);
        for(AioSession<T> aioSession:groupUnit.groupList){
            try {
                aioSession.writeBuffer().write(t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class GroupUnit {
        Set<AioSession<T>> groupList = new HashSet<>();
    }
}
