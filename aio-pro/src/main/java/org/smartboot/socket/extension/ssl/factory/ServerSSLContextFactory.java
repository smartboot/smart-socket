package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class ServerSSLContextFactory implements SSLContextFactory {
    private final InputStream keyStoreInputStream;
    private final String keyStorePassword;
    private final String keyPassword;

    public ServerSSLContextFactory(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword) {
        this.keyStoreInputStream = keyStoreInputStream;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
    }

    @Override
    public SSLContext create() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keyStoreInputStream, keyStorePassword.toCharArray());
        kmf.init(ks, keyPassword.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, new SecureRandom());
        return sslContext;
    }
}