/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Net.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.c1000k;

/**
 * fs.file-max = 4000000
 * fs.nr_open = 4000000
 * net.ipv4.ip_local_port_range = 1024 65535
 * net.ipv4.tcp_wmem = 4096 87380 4161536
 * net.ipv4.tcp_rmem = 4096 87380 4161536
 * net.ipv4.tcp_mem = 786432 2097152 3145728
 * fs.file-max = 4000000
 * @author 三刀
 * @version V1.0 , 2019/2/24
 */
public class Net {
    public static void main(String[] args) {
        for (int i = 1; i <= 20; i++) {
            System.out.println("ifconfig eth0:" + i + " 192.168.2." + i + " netmask 255.255.255.0 up");
        }
    }
}
