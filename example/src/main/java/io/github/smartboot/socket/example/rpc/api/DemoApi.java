/*******************************************************************************
 * Copyright (c) 2017-2026, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DemoApi.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.rpc.api;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public interface DemoApi {

    String test(String name);

    int sum(int a, int b);
}
