/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: BigObject.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket.decoder;

import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/22
 */
public class BigObject {
    private InputStream inputStream;

    public BigObject(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
