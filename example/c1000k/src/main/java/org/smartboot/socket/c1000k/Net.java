package org.smartboot.socket.c1000k;

/**
 * @author 三刀
 * @version V1.0 , 2019/2/24
 */
public class Net {
    public static void main(String[] args) {
        for (int i = 1; i <= 20; i++) {
            System.out.println("sudo ifconfig eth0:" + i + " 192.168.2." + i + " netmask 255.255.255.0 up");
        }
    }
}
