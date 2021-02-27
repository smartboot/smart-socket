/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DemoApiImpl.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.rpc.api;

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
