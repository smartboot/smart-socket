package org.smartboot.socket.example.api;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class DemoApiImpl implements DemoApi {
    @Override
    public String test(String name) {
        return "hello " + name;
    }

    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
