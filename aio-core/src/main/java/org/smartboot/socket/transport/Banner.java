/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: Banner.java
 * Date: 2018-01-26
 * Author: sandao
 */

package org.smartboot.socket.transport;

import java.io.PrintStream;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/26
 */
class Banner {
    public static final String BANNER_1 = "                               _                           _             _   \n" +
            "                              ( )_                        ( )           ( )_ \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)     ___    _      ___ | |/')    __  | ,_)\n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     /',__) /'_`\\  /'___)| , <   /'__`\\| |  \n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    \\__, \\( (_) )( (___ | |\\`\\ (  ___/| |_ \n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (____/`\\___/'`\\____)(_) (_)`\\____)`\\__)";

    public static void printBanner(PrintStream out) {
        out.println(BANNER_1);
        out.println(" :: smart-socket ::\t(v1.3.2)");
    }

}
