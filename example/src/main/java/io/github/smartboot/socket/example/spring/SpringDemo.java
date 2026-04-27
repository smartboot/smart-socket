/*******************************************************************************
 * Copyright (c) 2017-2026, tech.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SpringDemo.java
 * Date: 2026-04-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package io.github.smartboot.socket.example.spring;

import io.github.smartboot.socket.MessageProcessor;
import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.transport.AioQuickServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/1
 */
@Component
public class SpringDemo {

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private Protocol protocol;

    private AioQuickServer aioQuickServer;

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
        SpringDemo demo = context.getBean("springDemo", SpringDemo.class);
        System.out.println("服务启动成功：" + demo.aioQuickServer);
        ((ClassPathXmlApplicationContext) context).close();
        System.out.println("服务关闭");
    }

    @PostConstruct
    public void init() {
        this.aioQuickServer = new AioQuickServer(8080, protocol, messageProcessor);
        try {
            aioQuickServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @PreDestroy
    public void destroy() {
        aioQuickServer.shutdown();
    }
}
