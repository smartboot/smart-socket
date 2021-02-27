/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DemoApi.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.rpc.api;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public interface DemoApi {

    String test(String name);

    int sum(int a, int b);
}
