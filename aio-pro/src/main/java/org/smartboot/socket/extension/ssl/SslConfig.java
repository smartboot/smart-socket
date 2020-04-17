/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslConfig.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.extension.ssl;

import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SslConfig {
    /**
     * 配置引擎在握手时使用客户端（或服务器）模式
     */
    private boolean clientMode;
    private InputStream keyFile;

    private String keystorePassword;

    private String keyPassword;
    private InputStream trustFile;

    private String trustPassword;

    private ClientAuth clientAuth = ClientAuth.NONE;


    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public InputStream getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(InputStream keyFile) {
        this.keyFile = keyFile;
    }

    public InputStream getTrustFile() {
        return trustFile;
    }

    public void setTrustFile(InputStream trustFile) {
        this.trustFile = trustFile;
    }

    public String getTrustPassword() {
        return trustPassword;
    }

    public void setTrustPassword(String trustPassword) {
        this.trustPassword = trustPassword;
    }

    boolean isClientMode() {
        return clientMode;
    }

    void setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
    }

    ClientAuth getClientAuth() {
        return clientAuth;
    }

    void setClientAuth(ClientAuth clientAuth) {
        this.clientAuth = clientAuth;
    }

}
