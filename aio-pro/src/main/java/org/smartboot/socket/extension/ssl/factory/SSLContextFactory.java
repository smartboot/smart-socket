package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.SSLContext;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/20
 */
public interface SSLContextFactory {
    SSLContext create() throws Exception;
}
