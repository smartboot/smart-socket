/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: State.java
 * Date: 2018-02-27
 * Author: sandao
 */

package org.smartboot.socket.http.websocket;

enum State {
    READING_FIRST,
    READING_SECOND,
    READING_SIZE,
    MASKING_KEY,
    PAYLOAD,
    CORRUPT
}