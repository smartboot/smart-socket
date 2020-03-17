/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: NullSelectionKey.java
 * Date: 2020-03-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class UdpNullSelectionKey extends SelectionKey {

    @Override
    public SelectableChannel channel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Selector selector() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int interestOps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SelectionKey interestOps(int ops) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readyOps() {
        throw new UnsupportedOperationException();
    }
}