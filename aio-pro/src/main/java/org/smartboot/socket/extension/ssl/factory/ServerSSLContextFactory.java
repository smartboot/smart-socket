package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class ServerSSLContextFactory implements SSLContextFactory {
    private final InputStream keyStoreInputStream;
    private final String keyStorePassword;
    private final String keyPassword;
    private final InputStream trustInputStream;
    private final String trustPassword;

    public ServerSSLContextFactory(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword) {
        this(keyStoreInputStream, keyStorePassword, keyPassword, null, null);
    }

    public ServerSSLContextFactory(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword, 
                                 InputStream trustInputStream, String trustPassword) {
        this.keyStoreInputStream = keyStoreInputStream;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.trustInputStream = trustInputStream;
        this.trustPassword = trustPassword;
    }

    @Override
    public SSLContext create() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        if (keyStoreInputStream == null) {
            throw new IllegalArgumentException("keyStoreInputStream is null");
        }
        ks.load(keyStoreInputStream, keyStorePassword.toCharArray());
        
        // 确保证书链完整
        if (ks.size() == 0) {
            throw new IllegalStateException("Empty server certificate chain");
        }
        
        kmf.init(ks, keyPassword.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();

        // Initialize TrustManagers
        TrustManager[] trustManagers = null;
        if (trustInputStream != null) {
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(trustInputStream, trustPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);
            trustManagers = tmf.getTrustManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }
}