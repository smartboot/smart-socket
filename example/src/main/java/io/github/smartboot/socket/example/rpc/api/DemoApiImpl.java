/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DemoApiImpl.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.rpc.api;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class DemoApiImpl implements DemoApi {
    @Override
    public String test(String name) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(name);
        return "hello " + name;
    }

    @Override
    public int sum(int a, int b) {
        System.out.println(a + " " + b);
        return a + b;
    }
}
