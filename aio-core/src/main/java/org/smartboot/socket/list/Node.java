package org.smartboot.socket.list;

import org.smartboot.socket.transport.AioSession;

public class Node {
    AioSession session;
    int size;
    NodeStatus status;

    Node() {
    }

    public AioSession getSession() {
        return session;
    }

    public void setSession(AioSession session) {
        this.session = session;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}